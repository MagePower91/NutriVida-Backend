package cl.nutrivida.ms.citas.dto;

/** Un bloque de una hora en la agenda del día: si se puede reservar y, si no, por qué. */
public record BloqueHorario(String hora, boolean disponible, String motivo) { }
