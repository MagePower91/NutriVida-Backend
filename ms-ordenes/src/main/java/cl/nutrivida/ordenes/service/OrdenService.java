//El pago y las órdenes

package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
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

    public OrdenService(OrdenRepository ordenes, ItemCarritoRepository items, CatalogoClient catalogo) {
        this.ordenes = ordenes;
        this.items = items;
        this.catalogo = catalogo;
    }

    @Transactional
    public OrdenResponse pagar(Jwt jwt, CheckoutRequest req) {
        String run = jwt.getSubject();

        // 1. Tarjeta no vencida (el formato ya lo validó @Pattern)
        validarVencimiento(req.vencimiento());

        // 2. Qué se paga: lo seleccionado o todo el carrito
        List<ItemCarrito> todos = items.findByRunPacienteOrderByFechaHoraAsc(run);
        List<ItemCarrito> aPagar = (req.itemIds() == null || req.itemIds().isEmpty())
                ? todos
                : todos.stream().filter(i -> req.itemIds().contains(i.getId())).toList();
        if (aPagar.isEmpty()) {
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

        // 5. Descontar cupos de los talleres en ms-catalogo
        cuposPedidos.forEach((codigo, cantidad) -> catalogo.reservarCupos(codigo, cantidad, jwt.getTokenValue()));

        // 6. Crear la orden con los precios ACTUALES del catálogo
        Orden orden = new Orden();
        orden.setRunPaciente(run);
        orden.setCorreoPaciente(jwt.getClaimAsString("correo"));
        orden.setFecha(LocalDateTime.now());
        orden.setEstado("PAGADA");
        orden.setUltimos4Tarjeta(req.numero().substring(12));

        int total = 0;
        for (ItemCarrito item : aPagar) {
            ServicioCatalogo s = servicios.get(item.getCodigoServicio());
            orden.agregarDetalle(new DetalleOrden(s.codigo(), s.nombre(), s.precioFinal(),
                    item.getFechaHora(), item.getNutricionista()));
            total += s.precioFinal();
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