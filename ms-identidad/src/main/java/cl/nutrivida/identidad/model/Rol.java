package cl.nutrivida.identidad.model;

import cl.nutrivida.identidad.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.text.Normalizer;

public enum Rol {
    ADMINISTRADOR("Administrador"),
    RECEPCION("Recepción"),
    NUTRICIONISTA("Nutricionista"),
    PACIENTE("Paciente");

    private final String etiqueta;

    Rol(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }

    public static Rol desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de usuario es obligatorio.");
        }
        String buscado = sinTildes(texto.trim());
        for (Rol rol : values()) {
            if (rol.name().equalsIgnoreCase(buscado) || sinTildes(rol.etiqueta).equalsIgnoreCase(buscado)) {
                return rol;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de usuario no válido: " + texto);
    }

    private static String sinTildes(String t) {
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}