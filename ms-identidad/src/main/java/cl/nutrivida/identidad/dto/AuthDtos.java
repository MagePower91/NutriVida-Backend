//Agrupa en un solo archivo los datos que entran y salen del login.

package cl.nutrivida.identidad.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String correo,
            @NotBlank(message = "La contraseña es obligatoria.")
            String contrasena) { }

    public record RegistroRequest(
            @NotBlank(message = "El RUN es obligatorio.")
            @Pattern(regexp = "^[0-9]{6,8}[0-9kK]$", message = "Ingresa el RUN sin puntos ni guion.")
            String run,
            @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
            @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.") @Size(max = 100)
            String correo,
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres.")
            String contrasena,
            @Past(message = "La fecha de nacimiento debe ser anterior a hoy.") LocalDate fechaNacimiento,
            @Size(max = 60) String region,
            @Size(max = 60) String comuna,
            @Size(max = 300) String direccion) { }

    public record RefreshRequest(
            @NotBlank(message = "El refreshToken es obligatorio.")
            String refreshToken) { }

    public record AuthResponse(
            String accessToken, String refreshToken, String tipo,
            long expiraEnSegundos, UsuarioResponse usuario) { }
}