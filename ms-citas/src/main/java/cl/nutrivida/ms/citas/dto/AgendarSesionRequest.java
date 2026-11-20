
package cl.nutrivida.ms.citas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendarSesionRequest(

        @NotNull(message = "La fecha y hora son obligatorias.")
        @Future(message = "La fecha de la sesión debe ser futura.")
        LocalDateTime fechaHora,

        String nutricionista

) {
}

