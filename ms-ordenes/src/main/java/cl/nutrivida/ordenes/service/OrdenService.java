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
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrdenService {

    private final OrdenRepository ordenes;
    private final ItemCarritoRepository items;
    private final CatalogoClient catalogo;
    private final CitasClient citas;

    public OrdenService(OrdenRepository ordenes, ItemCarritoRepository items, CatalogoClient catalogo, CitasClient citas) {
        this.ordenes = ordenes;
        this.items = items;
        this.catalogo = catalogo;
        this.citas = citas;
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

        // 3. Volver a consultar el catálogo: precio actual, disponibilidad y cupos.
        //    Si UNA atención falla, no se cobra nada.
        Map<String, ServicioCatalogo> servicios = new HashMap<>();
        Map<String, Integer> cuposPedidos = new LinkedHashMap<>();
        for (ItemCarrito item : aPagar) {
            ServicioCatalogo s = servicios.computeIfAbsent(item.getCodigoServicio(), catalogo::obtenerServicio);
            if (!s.activo()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "No se realizó ningún cobro: «" + s.nombre() + "» ya no está disponible.");
            }
            if (s.cupos() != null) {
                cuposPedidos.merge(s.codigo(), 1, Integer::sum);
            }
        }
        cuposPedidos.forEach((codigo, pedidos) -> {
            ServicioCatalogo s = servicios.get(codigo);
            if (s.cupos() < pedidos) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "No se realizó ningún cobro: no quedan cupos suficientes en «" + s.nombre() + "».");
            }
        });

        // 4. Pago simulado: tarjetas terminadas en 0000 son rechazadas
        if (req.numero().endsWith("0000")) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "El banco rechazó la tarjeta. No se realizó ningún cobro.");
        }

        // 5. Crear la orden (para tener su número) y registrar lo pagado en la agenda (ms-citas):
        //    citas confirmadas, planes contratados y citas con pago en clínica ya pagadas.
        //    Si ms-citas rechaza una atención (horario ocupado, menos de 2 h...), se lanza un error,
        //    la transacción deshace la orden y no se descuentan cupos ni se cobra.
        Orden orden = new Orden();
        orden.setRunPaciente(run);
        orden.setCorreoPaciente(jwt.getClaimAsString("correo"));
        orden.setFecha(LocalDateTime.now());
        orden.setEstado("PAGADA");
        orden.setUltimos4Tarjeta(req.numero().substring(12));
        orden.setTotal(0);
        ordenes.save(orden);

        List<ItemPagado> pagados = aPagar.stream()
                .map(i -> new ItemPagado(i.getCodigoServicio(), i.getFechaHora(), i.getNutricionista(), i.getMotivo()))
                .toList();
        PagoResponse agenda = citas.registrarPago(new PagoRequest(String.valueOf(orden.getId()),
                jwt.getClaimAsString("correo"), req.correoConfirmacion(), pagados, citaIds));

        // 6. Descontar cupos de los talleres en ms-catalogo
        cuposPedidos.forEach((codigo, cantidad) -> catalogo.reservarCupos(codigo, cantidad));

        // 7. El detalle (boleta) con lo que efectivamente se agendó y su precio actual
        int total = 0;
        for (LineaPagada linea : agenda.lineas()) {
            int precio = linea.precio() == null ? 0 : linea.precio().intValue();
            DetalleOrden detalle = new DetalleOrden(linea.codigoServicio(), linea.nombreServicio(), precio,
                    linea.fechaHora(), linea.nutricionista());
            detalle.setCitaId(linea.citaId());
            detalle.setPlanId(linea.planId());
            orden.agregarDetalle(detalle);
            total += precio;
        }
        orden.setTotal(total);

        ordenes.save(orden);
        items.deleteAll(aPagar);            // las atenciones pagadas salen del carrito
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