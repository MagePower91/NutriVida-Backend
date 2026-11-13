//los datos para crear o editar un usuario desde el panel de administración.
// Al editar, si la contraseña viene vacía, se mantiene la actual.

package cl.nutrivida.identidad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UsuarioRequest(
        @NotBlank(message = "El RUN es obligatorio.")
        @Pattern(regexp = "^[0-9]{6,8}[0-9kK]$", message = "Ingresa el RUN sin puntos ni guion.")
        String run,
        @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "Ingresa un correo válido.") @Size(max = 100)
        String correo,
        @Schema(description = "Obligatoria al crear. Vacía al editar = se mantiene la actual.")
        @Size(max = 10, message = "La contraseña no puede superar los 10 caracteres.")
        String contrasena,
        @Schema(example = "Recepción", allowableValues = {"Administrador", "Recepción", "Nutricionista", "Paciente"})
        @NotBlank(message = "El tipo de usuario es obligatorio.")
        String tipoUsuario,
        @Size(max = 60) String region,
        @Size(max = 60) String comuna,
        @Size(max = 300) String direccion,
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy.") LocalDate fechaNacimiento) { }