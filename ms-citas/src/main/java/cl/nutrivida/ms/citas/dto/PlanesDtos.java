package cl.nutrivida.ms.citas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class PlanesDtos {

    private PlanesDtos() {
    }

    // Datos enviados por ms-ordenes después de confirmar el pago.
    public record RegistrarPlanRequest(
            @NotBlank
            @Size(max = 100)
            String referenciaCompra,

            @NotBlank
            @Size(max = 50)
            String ordenId,

            @NotBlank
            @Size(max = 150)
            String pacienteCorreo,

            @NotBlank
            @Size(max = 5)
            String codigoServicio,

            @NotBlank
            @Size(max = 100)
            String nombreServicio,

            @NotNull
            @Min(1)
            Integer sesionesTotales
    ) {
    }

    // Información que recibe el paciente al consultar su plan.
    public record PlanResponse(
            Long id,
            String referenciaCompra,
            String ordenId,
            String codigoServicio,
            String nombreServicio,
            int sesionesTotales,
            int sesionesAtendidas,
            int sesionesAgendadas,
            int sesionesDisponibles,
            boolean completado,
            LocalDateTime creadoEn
    ) {
    }
}
