//Es lo que la API devuelve de un usuario. Nunca incluye la contraseña.

package cl.nutrivida.identidad.dto;

import cl.nutrivida.identidad.model.Usuario;
import java.time.LocalDate;

public record UsuarioResponse(
        String run, String nombre, String apellidos, String correo, String tipoUsuario,
        String region, String comuna, String direccion, LocalDate fechaNacimiento, boolean activo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol().getEtiqueta(), u.getRegion(), u.getComuna(), u.getDireccion(),
                u.getFechaNacimiento(), u.isActivo());
    }
}