package cl.nutrivida.ms.citas.model;

/** Estado de un plan contratado. Finaliza solo cuando todas sus sesiones fueron atendidas. */
public enum EstadoPlan {
    ACTIVO,
    FINALIZADO,
    CANCELADO
}
