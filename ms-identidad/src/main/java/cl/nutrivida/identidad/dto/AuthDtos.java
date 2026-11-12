//Agrupa en un solo archivo los datos que entran y salen del login.

package cl.nutrivida.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String correo,
            @NotBlank(message = "La contraseña es obligatoria.")
            String contrasena) { }

    public record RefreshRequest(
            @NotBlank(message = "El refreshToken es obligatorio.")
            String refreshToken) { }

    public record AuthResponse(
            String accessToken, String refreshToken, String tipo,
            long expiraEnSegundos, UsuarioResponse usuario) { }
}