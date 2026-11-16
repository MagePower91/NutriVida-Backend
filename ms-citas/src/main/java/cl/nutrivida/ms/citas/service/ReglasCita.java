// Reglas de negocio de las citas, sin base de datos ni Spring: se prueban de forma aislada.
// Son las mismas reglas que aplica el frontend (src/domain/citas.js), numeradas como en el ERS.

package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPago;
import cl.nutrivida.ms.citas.model.MetodoPago;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class ReglasCita {

    public static final int ANTICIPACION_MINIMA_HORAS = 2;   // RN-03 y RN-09
    public static final int PLAZO_CONFIRMACION_HORAS = 24;   // RN-02 y RN-07
    public static final List<EstadoCita> ESTADOS_ACTIVOS = List.of(EstadoCita.POR_CONFIRMAR, EstadoCita.CONFIRMADA);

    private ReglasCita() { }

    /** Estado con el que nace (o queda al reprogramar) una cita, y hasta cuándo se puede confirmar. */
    public record Evaluacion(EstadoCita estadoCita, LocalDateTime limiteConfirmacion) { }

    public static double horasHasta(LocalDateTime fechaHora, LocalDateTime ahora) {
        return Duration.between(ahora, fechaHora).toMinutes() / 60.0;
    }

    // RN-03: no se reserva en línea con menos de 2 h. RN-02/RN-07: con pago en clínica y 24 h o más
    // queda "Por confirmar" hasta 24 h antes; con menos de 24 h, reservar ya es la confirmación.
    // Pagada online queda confirmada de inmediato (RN-05).
    public static Evaluacion evaluarReserva(LocalDateTime fechaHora, MetodoPago metodo, LocalDateTime ahora) {
        if (horasHasta(fechaHora, ahora) < ANTICIPACION_MINIMA_HORAS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Con menos de " + ANTICIPACION_MINIMA_HORAS
                    + " h de anticipación no se puede reservar en línea. Llama a la clínica.");
        }
        if (metodo == MetodoPago.ONLINE || horasHasta(fechaHora, ahora) < PLAZO_CONFIRMACION_HORAS) {
            return new Evaluacion(EstadoCita.CONFIRMADA, null);
        }
        return new Evaluacion(EstadoCita.POR_CONFIRMAR, fechaHora.minusHours(PLAZO_CONFIRMACION_HORAS));
    }

    public static boolean estaActiva(Cita cita) {
        return ESTADOS_ACTIVOS.contains(cita.getEstadoCita());
    }

    // RN-09: el paciente puede cancelar o reprogramar hasta 2 h antes.
    public static boolean puedeCambiarPaciente(Cita cita, LocalDateTime ahora) {
        return estaActiva(cita) && horasHasta(cita.getFechaHora(), ahora) >= ANTICIPACION_MINIMA_HORAS;
    }

    // RN-06: una cita "Por confirmar" se cancela si pasó su límite de confirmación.
    public static boolean debeCancelarsePorFaltaDeConfirmacion(Cita cita, LocalDateTime ahora) {
        return cita.getEstadoCita() == EstadoCita.POR_CONFIRMAR
                && cita.getLimiteConfirmacion() != null
                && ahora.isAfter(cita.getLimiteConfirmacion());
    }

    // RN-11: una cita pagada online que termina cancelada o en inasistencia se devuelve completa.
    public static BigDecimal reembolsoPendiente(Cita cita) {
        boolean pagadaOnline = cita.getMetodoPago() == MetodoPago.ONLINE && cita.getEstadoPago() == EstadoPago.PAGADO;
        boolean sinAtencion = cita.getEstadoCita() == EstadoCita.CANCELADA || cita.getEstadoCita() == EstadoCita.INASISTENCIA;
        return pagadaOnline && sinAtencion && cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
    }
}
