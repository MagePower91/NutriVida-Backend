package cl.nutrivida.ms.citas.dto;

import cl.nutrivida.ms.citas.model.Cita;

import java.math.BigDecimal;

/** La cita cancelada y cuánto se debe devolver al paciente (0 si no pagó online). */
public record CancelacionResponse(Cita cita, BigDecimal reembolsoPendiente) { }
