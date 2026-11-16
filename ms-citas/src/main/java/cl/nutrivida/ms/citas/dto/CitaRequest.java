// Datos que el cliente puede enviar al agendar. El resto (paciente, precio, estados, pagos)
// lo define el servidor: así nadie puede marcar una cita como pagada o ponerle otro precio.

package cl.nutrivida.ms.citas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaRequest(
        @Schema(example = "CN001")
        @NotBlank(message = "El servicio es obligatorio.")
        String codigoServicio,

        @Schema(example = "Carolina Fuentes", description = "Opcional: nutricionista preferido")
        @Size(max = 100)
        String nutricionista,

        @Schema(example = "2026-11-20T10:00:00")
        @NotNull(message = "La fecha y hora es obligatoria.")
        @Future(message = "La fecha y hora debe ser futura.")
        LocalDateTime fechaHora,

        @Schema(example = "Bajar de peso")
        @Size(max = 1000)
        String motivo,

        @Email(message = "Ingresa un correo de confirmación válido.")
        @Size(max = 100)
        String correoConfirmacion,

        @Schema(description = "Solo administración y recepción: correo del paciente al que se le agenda")
        @Email(message = "Ingresa un correo de paciente válido.")
        String pacienteCorreo) { }
