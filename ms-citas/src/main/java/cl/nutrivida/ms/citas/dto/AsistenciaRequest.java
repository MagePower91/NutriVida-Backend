package cl.nutrivida.ms.citas.dto;

import cl.nutrivida.ms.citas.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AsistenciaRequest(
        @Schema(example = "ATENDIDA", description = "ATENDIDA o INASISTENCIA")
        @NotNull(message = "Indica si el paciente asistió (ATENDIDA) o no (INASISTENCIA).")
        EstadoCita estado) { }
