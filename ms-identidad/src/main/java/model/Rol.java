package cl.nutrivida.identidad.model;

public enum Rol {
    ADMINISTRADOR("Administrador"),
    RECEPCION("Recepción"),
    NUTRICIONISTA("Nutricionista"),
    PACIENTE("Paciente");

    private final String etiqueta;

    Rol(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}