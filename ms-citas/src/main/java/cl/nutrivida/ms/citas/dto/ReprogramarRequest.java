package cl.nutrivida.ms.citas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReprogramarRequest(
        @Schema(example = "2026-11-25T11:00:00")
        @NotNull(message = "La nueva fecha y hora es obligatoria.")
        @Future(message = "La nueva fecha y hora debe ser futura.")
        LocalDateTime fechaHora,

        @Schema(example = "Carolina Fuentes", description = "Opcional: vacío = cualquier nutricionista libre")
        @Size(max = 100)
        String nutricionista) { }
