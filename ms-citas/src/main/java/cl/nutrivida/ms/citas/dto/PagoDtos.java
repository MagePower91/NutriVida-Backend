// Datos del pago interno: ms-ordenes los envía al confirmar el pago del carrito, con su token de servicio.
// No lo puede llamar el navegador (el gateway lo bloquea y el endpoint exige el rol SERVICIO).

package cl.nutrivida.ms.citas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PagoDtos {

    private PagoDtos() { }

    /** Una atención del carrito que se acaba de pagar. Un plan puede venir sin fecha (sin primera sesión). */
    public record ItemPagado(
            @Schema(example = "CN001") @NotBlank String codigoServicio,
            @Schema(example = "2026-11-20T10:00:00") LocalDateTime fechaHora,
            @Schema(example = "Carolina Fuentes") @Size(max = 100) String nutricionista,
            @Size(max = 1000) String motivo,
            @Schema(example = "C-12", description = "Obligatoria en un plan: evita registrarlo dos veces")
            @Size(max = 100) String referenciaCompra) {

        // Mantiene compatibilidad con los lugares que todavía envían cuatro parámetros.
        public ItemPagado(String codigoServicio, LocalDateTime fechaHora, String nutricionista, String motivo) {
            this(codigoServicio, fechaHora, nutricionista, motivo, null);
        }
    }

    public record PagoRequest(
            @Schema(example = "15") @NotBlank String ordenId,
            @Schema(example = "paciente@gmail.com") @NotBlank @Email String pacienteCorreo,
            @Email String correoConfirmacion,
            @Valid List<ItemPagado> items,
            @Schema(description = "Citas ya agendadas con pago en clínica que se pagan online en esta orden")
            List<Long> citaIds) { }

    /** Lo que se pagó, línea por línea, con el precio que define el catálogo (o la cita). */
    public record LineaPagada(Long citaId, Long planId, String codigoServicio, String nombreServicio,
                              LocalDateTime fechaHora, String nutricionista, BigDecimal precio) { }

    public record PagoResponse(List<LineaPagada> lineas, BigDecimal total) { }

    /** Para agendar una sesión de un plan ya pagado. */
    public record SesionRequest(
            @Schema(example = "2026-11-25T11:00:00") @NotNull(message = "La fecha y hora es obligatoria.") LocalDateTime fechaHora,
            @Schema(example = "Carolina Fuentes", description = "Opcional: vacío = cualquier nutricionista libre")
            @Size(max = 100) String nutricionista) { }

    /** Un plan con el resumen de sus sesiones (RN-13). */
    public record PlanResponse(Long id, String pacienteCorreo, String codigoServicio, String nombre,
                               int sesionesTotales, BigDecimal montoPagado, String estadoPlan, String ordenId,
                               LocalDateTime creadoEn, int atendidas, int agendadas, int porAgendar) { }
}
