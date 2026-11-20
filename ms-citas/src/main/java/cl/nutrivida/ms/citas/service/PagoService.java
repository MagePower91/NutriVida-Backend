// Une el pago del carrito (ms-ordenes) con la agenda: al pagar, cada atención se convierte en una cita
// confirmada y pagada online, cada plan queda contratado (con su primera sesión si se eligió horario)
// y las citas con pago en clínica que se eligieron quedan pagadas.
// Todo o nada: si una atención ya no se puede agendar, no se guarda ninguna y ms-ordenes no cobra.

package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.client.CatalogoClient;
import cl.nutrivida.ms.citas.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ms.citas.dto.PagoDtos.ItemPagado;
import cl.nutrivida.ms.citas.dto.PagoDtos.LineaPagada;
import cl.nutrivida.ms.citas.dto.PagoDtos.PagoRequest;
import cl.nutrivida.ms.citas.dto.PagoDtos.PagoResponse;
import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.exception.HorarioNoLaboralException;
import cl.nutrivida.ms.citas.exception.HorarioOcupadoException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPago;
import cl.nutrivida.ms.citas.model.EstadoPlan;
import cl.nutrivida.ms.citas.model.MetodoPago;
import cl.nutrivida.ms.citas.model.Plan;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PagoService {

    private final CitaRepository citaRepository;
    private final PlanRepository planRepository;
    private final CatalogoClient catalogoClient;
    private final CitaService citaService;
    private final Clock reloj;

    public PagoService(CitaRepository citaRepository, PlanRepository planRepository, CatalogoClient catalogoClient,
                       CitaService citaService, Clock reloj) {
        this.citaRepository = citaRepository;
        this.planRepository = planRepository;
        this.catalogoClient = catalogoClient;
        this.citaService = citaService;
        this.reloj = reloj;
    }

    @Transactional
    public PagoResponse registrarPagoOnline(PagoRequest datos) {
        String paciente = datos.pacienteCorreo().trim().toLowerCase();
        String correoConfirmacion = textoVacio(datos.correoConfirmacion()) ? paciente : datos.correoConfirmacion().trim();
        List<ItemPagado> items = datos.items() == null ? List.of() : datos.items();
        List<Long> citaIds = datos.citaIds() == null ? List.of() : datos.citaIds();
        if (items.isEmpty() && citaIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay atenciones para pagar.");
        }

        List<LineaPagada> lineas = new ArrayList<>();
        for (ItemPagado item : items) {
            ServicioCatalogo servicio = catalogoClient.obtenerServicio(item.codigoServicio());
            if (!servicio.activo()) {
                throw new ApiException(HttpStatus.CONFLICT, "«" + servicio.nombre() + "» ya no está disponible.");
            }
            lineas.add(servicio.esPlan()
                    ? contratarPlan(servicio, item, paciente, correoConfirmacion, datos.ordenId())
                    : agendarPagada(servicio, item, paciente, correoConfirmacion, datos.ordenId()));
        }
        for (Long citaId : citaIds) {
            lineas.add(pagarCitaPendiente(citaId, paciente, datos.ordenId()));
        }

        BigDecimal total = lineas.stream().map(LineaPagada::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PagoResponse(lineas, total);
    }

    // Una consulta, evaluación o taller pagado online: nace confirmado (RN-05).
    private LineaPagada agendarPagada(ServicioCatalogo servicio, ItemPagado item, String paciente,
                                      String correoConfirmacion, String ordenId) {
        if (item.fechaHora() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Falta la fecha y hora de «" + servicio.nombre() + "».");
        }
        BigDecimal precio = BigDecimal.valueOf(servicio.precioFinal());
        Cita cita = nuevaCitaOnline(servicio.codigo(), servicio.nombre(), paciente, correoConfirmacion,
                item.fechaHora(), item.motivo(), ordenId, null, precio);
        try {
            citaService.asignarHorario(cita, item.nutricionista(), servicio.cupos());
        } catch (HorarioOcupadoException | HorarioNoLaboralException e) {
            // Se agrega el nombre del servicio, para que el paciente sepa cuál cambiar en su carrito.
            throw new ApiException(HttpStatus.CONFLICT, "«" + servicio.nombre() + "»: " + e.getMessage());
        }
        Cita guardada = citaRepository.save(cita);
        return linea(guardada, null, precio);
    }

   
// RN-13: el plan se paga completo una vez. La primera sesión es opcional.
private LineaPagada contratarPlan(ServicioCatalogo servicio, ItemPagado item, String paciente,
                                  String correoConfirmacion, String ordenId) {

    if (textoVacio(item.referenciaCompra())) {
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Falta la referencia de compra del plan «" + servicio.nombre() + "»."
        );
    }

    // Si ya existe un plan con esta referencia, no lo volvemos a crear.
    Plan existente = planRepository.findByReferenciaCompra(item.referenciaCompra()).orElse(null);

    if (existente != null) {
        if (!existente.getPacienteCorreo().equalsIgnoreCase(paciente)
                || !existente.getCodigoServicio().equals(servicio.codigo())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La referencia de compra ya está asociada a otro plan."
            );
        }

        // Recuperamos la primera sesión existente, si el plan tenía una.
        List<Cita> sesiones = citaRepository.findByPlanId(String.valueOf(existente.getId()));

        if (!sesiones.isEmpty()) {
            Cita sesion = sesiones.get(0);
            return new LineaPagada(
                    sesion.getId(),
                    existente.getId(),
                    servicio.codigo(),
                    existente.getNombre(),
                    sesion.getFechaHora(),
                    sesion.getNutricionista(),
                    existente.getMontoPagado()
            );
        }

        return new LineaPagada(
                null,
                existente.getId(),
                servicio.codigo(),
                existente.getNombre(),
                null,
                null,
                existente.getMontoPagado()
        );
    }

    // Si no existe, registramos el plan por primera vez.
    BigDecimal precio = BigDecimal.valueOf(servicio.precioFinal());

    Plan plan = new Plan();
    plan.setReferenciaCompra(item.referenciaCompra());
    plan.setPacienteCorreo(paciente);
    plan.setCodigoServicio(servicio.codigo());
    plan.setNombre(servicio.nombre());
    plan.setSesionesTotales(servicio.sesiones());
    plan.setMontoPagado(precio);
    plan.setEstadoPlan(EstadoPlan.ACTIVO);
    plan.setOrdenId(ordenId);
    plan.setMotivo(item.motivo());
    plan.setCreadoEn(ahora());

    Plan guardado = planRepository.save(plan);

    if (item.fechaHora() == null) {
        return new LineaPagada(
                null,
                guardado.getId(),
                servicio.codigo(),
                servicio.nombre(),
                null,
                null,
                precio
        );
    }

    Cita sesion = crearSesion(
            guardado,
            correoConfirmacion,
            item.fechaHora(),
            item.nutricionista()
    );

    return new LineaPagada(
            sesion.getId(),
            guardado.getId(),
            servicio.codigo(),
            servicio.nombre(),
            sesion.getFechaHora(),
            sesion.getNutricionista(),
            precio
    );
}


    // Sesión de un plan: confirmada y sin precio propio (el pago está en el plan).
    Cita crearSesion(Plan plan, String correoConfirmacion, LocalDateTime fechaHora, String nutricionista) {
        ReglasCita.evaluarReserva(fechaHora, MetodoPago.ONLINE, ahora());
        Cita sesion = nuevaCitaOnline(plan.getCodigoServicio(), plan.getNombre() + " (sesión)", plan.getPacienteCorreo(),
                correoConfirmacion, fechaHora, plan.getMotivo(), plan.getOrdenId(), String.valueOf(plan.getId()), BigDecimal.ZERO);
        citaService.asignarHorario(sesion, nutricionista, null);
        return citaRepository.save(sesion);
    }

    // Una cita agendada con pago en clínica que el paciente paga ahora online: queda pagada y confirmada.
    private LineaPagada pagarCitaPendiente(Long citaId, String paciente, String ordenId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La cita " + citaId + " no existe."));
        if (!cita.getPacienteCorreo().equalsIgnoreCase(paciente)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La cita " + citaId + " no es tuya.");
        }
        if (!ReglasCita.estaActiva(cita) || cita.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "La cita de «" + cita.getNombreServicio() + "» ya no tiene un pago pendiente.");
        }
        cita.setMetodoPago(MetodoPago.ONLINE);
        cita.setEstadoPago(EstadoPago.PAGADO);
        cita.setMontoPagado(cita.getPrecio());
        cita.setEstadoCita(EstadoCita.CONFIRMADA);
        cita.setLimiteConfirmacion(null);
        cita.setOrdenId(ordenId);
        Cita guardada = citaRepository.save(cita);
        return linea(guardada, null, guardada.getPrecio());
    }

    private Cita nuevaCitaOnline(String codigo, String nombre, String paciente, String correoConfirmacion,
                                 LocalDateTime fechaHora, String motivo, String ordenId, String planId, BigDecimal precio) {
        ReglasCita.evaluarReserva(fechaHora, MetodoPago.ONLINE, ahora());
        Cita cita = new Cita();
        cita.setPacienteCorreo(paciente);
        cita.setCorreoConfirmacion(correoConfirmacion);
        cita.setCodigoServicio(codigo);
        cita.setNombreServicio(nombre);
        cita.setMotivo(motivo);
        cita.setFechaHora(fechaHora);
        cita.setMetodoPago(MetodoPago.ONLINE);
        cita.setEstadoCita(EstadoCita.CONFIRMADA);
        cita.setEstadoPago(EstadoPago.PAGADO);
        cita.setLimiteConfirmacion(null);
        cita.setPrecio(precio);
        cita.setMontoPagado(precio);
        cita.setMontoReembolsado(BigDecimal.ZERO);
        cita.setOrdenId(ordenId);
        cita.setPlanId(planId);
        cita.setCreadaEn(ahora());
        cita.setReprogramaciones(0);
        return cita;
    }

    private static LineaPagada linea(Cita cita, Long planId, BigDecimal precio) {
        return new LineaPagada(cita.getId(), planId, cita.getCodigoServicio(), cita.getNombreServicio(),
                cita.getFechaHora(), cita.getNutricionista(), precio);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
