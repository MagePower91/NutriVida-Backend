//El pago y las órdenes

package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ordenes.client.CitasClient;
import cl.nutrivida.ordenes.client.CitasClient.ItemPagado;
import cl.nutrivida.ordenes.client.CitasClient.LineaPagada;
import cl.nutrivida.ordenes.client.CitasClient.PagoRequest;
import cl.nutrivida.ordenes.client.CitasClient.PagoResponse;
import cl.nutrivida.ordenes.dto.OrdenesDtos.CheckoutRequest;
import cl.nutrivida.ordenes.dto.OrdenesDtos.OrdenResponse;
import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.model.DetalleOrden;
import cl.nutrivida.ordenes.model.ItemCarrito;
import cl.nutrivida.ordenes.model.Orden;
import cl.nutrivida.ordenes.repository.ItemCarritoRepository;
import cl.nutrivida.ordenes.repository.OrdenRepository;
import cl.nutrivida.ordenes.repository.DetalleOrdenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    @Service
    public class OrdenService {

    
    private final OrdenRepository ordenes;
    private final ItemCarritoRepository items;
    private final CatalogoClient catalogo;
    private final CitasClient citasClient;
    private final DetalleOrdenRepository detallesOrden;

    public OrdenService(
            OrdenRepository ordenes,
            ItemCarritoRepository items,
            CatalogoClient catalogo,
            CitasClient citasClient,
            DetalleOrdenRepository detallesOrden
    ) {
        this.ordenes = ordenes;
        this.items = items;
        this.catalogo = catalogo;
        this.citasClient = citasClient;
        this.detallesOrden = detallesOrden;
    }

    @Transactional
    public OrdenResponse pagar(Jwt jwt, CheckoutRequest req) {
        String run = jwt.getSubject();

        // 1. Tarjeta no vencida (el formato ya lo validó @Pattern)
        validarVencimiento(req.vencimiento());

        // 2. Qué se paga: lo seleccionado (o todo el carrito si no se indica) y las citas con pago en clínica elegidas
        List<ItemCarrito> todos = items.findByRunPacienteOrderByFechaHoraAsc(run);
        List<ItemCarrito> aPagar = req.itemIds() == null
                ? todos
                : todos.stream().filter(i -> i.getId() != null && req.itemIds().contains(i.getId())).toList();
        List<Long> citaIds = req.citaIds() == null ? List.of() : req.citaIds();
        if (aPagar.isEmpty() && citaIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay atenciones para pagar en el carrito.");
        }

        // 3. Volver a consultar el catálogo: que cada servicio siga disponible. Si UNO falla, no se cobra nada.
        //    Los cupos de un taller son POR HORARIO (cuántas personas caben en cada sesión del taller) y los
        //    controla ms-citas al agendar: si ese horario ya está lleno, rechaza el pago en el paso 5.
        //    Por eso aquí no se descuentan cupos del catálogo, que es la capacidad fija de cada horario.
        Map<String, ServicioCatalogo> servicios = new HashMap<>();
        for (ItemCarrito item : aPagar) {
            ServicioCatalogo s = servicios.computeIfAbsent(item.getCodigoServicio(), catalogo::obtenerServicio);
            if (!s.activo()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "No se realizó ningún cobro: «" + s.nombre() + "» ya no está disponible.");
            }
        }

        // 4. Pago simulado: tarjetas terminadas en 0000 son rechazadas
        if (req.numero().endsWith("0000")) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "El banco rechazó la tarjeta. No se realizó ningún cobro.");
        }

        // 5. Crear la orden (para tener su número) y registrar lo pagado en la agenda (ms-citas):
        //    citas confirmadas, planes contratados y citas con pago en clínica ya pagadas.
        //    Si ms-citas rechaza una atención (horario ocupado, taller sin cupos en ese horario, menos de 2 h...),
        //    se lanza un error, la transacción deshace la orden y no se cobra.
        Orden orden = new Orden();
        orden.setRunPaciente(run);
        orden.setCorreoPaciente(jwt.getClaimAsString("correo"));
        orden.setFecha(LocalDateTime.now());
        orden.setEstado("PAGADA");
        orden.setUltimos4Tarjeta(req.numero().substring(12));
        orden.setTotal(0);
        ordenes.save(orden);

        List<ItemPagado> pagados = aPagar.stream()
        .map(i -> {
            ServicioCatalogo servicio = servicios.get(i.getCodigoServicio());

            // La referencia se mantiene estable mientras el artículo siga en el carrito.
            String referenciaCompra = servicio.esPlan()
                    ? "C-" + i.getId()
                    : null;

            return new ItemPagado(
                    i.getCodigoServicio(),
                    i.getFechaHora(),
                    i.getNutricionista(),
                    i.getMotivo(),
                    referenciaCompra
            );
        })
        .toList();

        PagoResponse agenda = citasClient.registrarPago(new PagoRequest(
                String.valueOf(orden.getId()),
                jwt.getClaimAsString("correo"),
                req.correoConfirmacion(),
                pagados,
                citaIds
        ));

        // 6. Crear los detalles de la boleta con lo que efectivamente registró ms-citas.
        int total = 0;
        int indiceItem = 0;

        for (LineaPagada linea : agenda.lineas()) {

            int precio = linea.precio() == null ? 0 : linea.precio().intValue();

            DetalleOrden detalle = new DetalleOrden(
                    linea.codigoServicio(),
                    linea.nombreServicio(),
                    precio,
                    linea.fechaHora(),
                    linea.nutricionista()
            );

            detalle.setCitaId(linea.citaId());
            detalle.setPlanId(linea.planId());

            // Las primeras líneas corresponden a los artículos del carrito.
            // Las siguientes pueden corresponder a citas que se pagaron en clínica.
            if (indiceItem < aPagar.size()) {
                ItemCarrito item = aPagar.get(indiceItem);
                ServicioCatalogo servicio = servicios.get(item.getCodigoServicio());

                if (servicio.esPlan()) {
                    detalle.setReferenciaCompra("C-" + item.getId());
                    detalle.setSesionesTotales(servicio.sesiones());
                    detalle.setEstadoRegistroPlan("REGISTRADO");
                }

                indiceItem++;
            }

            orden.agregarDetalle(detalle);
            total += precio;
        }

        orden.setTotal(total);
        ordenes.save(orden);

        items.deleteAll(aPagar);
        return OrdenResponse.de(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(Jwt jwt) {
        List<Orden> lista = esPersonal(jwt)
                ? ordenes.findAllByOrderByFechaDesc()
                : ordenes.findByRunPacienteOrderByFechaDesc(jwt.getSubject());
        return lista.stream().map(OrdenResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtener(Long id, Jwt jwt) {
        Orden orden = ordenes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Orden no encontrada."));
        if (!esPersonal(jwt) && !orden.getRunPaciente().equals(jwt.getSubject())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propias órdenes.");
        }
        return OrdenResponse.de(orden);
    }

    // ---------- reglas internas ----------

    @Transactional
    public int reintentarRegistroPlanesPendientes() {

        List<DetalleOrden> pendientes =
                detallesOrden.findByEstadoRegistroPlanAndOrden_Estado(
                        "PENDIENTE",
                        "PAGADA"
                );

        int registrados = 0;

        for (DetalleOrden detalle : pendientes) {

            Orden orden = detalle.getOrden();

            String referenciaCompra = detalle.getReferenciaCompra();

            if (referenciaCompra == null || referenciaCompra.isBlank()) {
                referenciaCompra = orden.getId() + "-" + detalle.getId();
                detalle.setReferenciaCompra(referenciaCompra);
            }

            try {
                citasClient.registrarPlan(new CitasClient.RegistrarPlanRequest(
                        referenciaCompra,
                        String.valueOf(orden.getId()),
                        orden.getCorreoPaciente(),
                        detalle.getCodigoServicio(),
                        detalle.getNombreServicio(),
                        detalle.getSesionesTotales()
                ));

                detalle.setEstadoRegistroPlan("REGISTRADO");
                registrados++;

            } catch (RuntimeException e) {
                System.err.println(
                        "Reintento fallido para el plan " + referenciaCompra
                                + ": " + e.getMessage()
                );
            }

            detallesOrden.save(detalle);
        }

        return registrados;
    }

    private boolean esPersonal(Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        return "ADMINISTRADOR".equals(rol) || "RECEPCION".equals(rol);
    }

    private void validarVencimiento(String mmaa) {
        int mes = Integer.parseInt(mmaa.substring(0, 2));
        int anio = 2000 + Integer.parseInt(mmaa.substring(3, 5));
        if (YearMonth.of(anio, mes).isBefore(YearMonth.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La tarjeta está vencida.");
        }
    }
}