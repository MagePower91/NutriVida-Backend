package cl.nutrivida.identidad.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "Indica true (activo) o false (desactivado).")
        Boolean activo) { }