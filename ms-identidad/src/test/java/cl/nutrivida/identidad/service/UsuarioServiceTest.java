package cl.nutrivida.identidad.service;

import cl.nutrivida.identidad.dto.UsuarioRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.exception.ApiException;
import cl.nutrivida.identidad.model.Rol;
import cl.nutrivida.identidad.model.Usuario;
import cl.nutrivida.identidad.repository.RefreshTokenRepository;
import cl.nutrivida.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas de negocio de usuarios.
 * Mockito reemplaza los repositorios y el encriptador: no se usa base de datos.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock PasswordEncoder encoder;
    @InjectMocks UsuarioService service;

    private Usuario admin(String run) {
        return new Usuario(run, "Admin", "NutriVida", run + "@gmail.com", "hash",
                Rol.ADMINISTRADOR, null, null, null);
    }

    private UsuarioRequest pacienteAna() {
        return new UsuarioRequest("211234563", "Ana", "Rojas", "ana@gmail.com", "ana123",
                "Paciente", null, null, null, null);
    }

    // Verifica que un administrador no pueda eliminar su propia cuenta (y que no se borre nada).
    @Test
    @DisplayName("Un administrador no puede eliminar su propia cuenta")
    void noPermiteEliminarSuPropiaCuenta() {
        when(usuarios.findById("761920839")).thenReturn(Optional.of(admin("761920839")));

        assertThatThrownBy(() -> service.eliminar("761920839", "761920839"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("propia cuenta");
        verify(usuarios, never()).delete(any());
    }

    // Verifica que no se pueda eliminar al último administrador activo del sistema.
    @Test
    @DisplayName("Debe quedar al menos un administrador activo")
    void noPermiteEliminarAlUltimoAdministrador() {
        when(usuarios.findById("123456785")).thenReturn(Optional.of(admin("123456785")));
        when(usuarios.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> service.eliminar("123456785", "761920839"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("al menos un administrador");
        verify(usuarios, never()).delete(any());
    }

    // Verifica que no se pueda registrar un usuario con un RUN que ya existe.
    @Test
    @DisplayName("No permite registrar un RUN que ya existe")
    void noPermiteRunRepetido() {
        when(usuarios.existsById("211234563")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(pacienteAna()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("RUN");
        verify(usuarios, never()).save(any());
    }

    // Verifica que la contraseña se guarde encriptada (BCrypt) y nunca en texto plano.
    @Test
    @DisplayName("Guarda la contraseña encriptada, nunca en texto plano")
    void guardaContrasenaEncriptada() {
        when(usuarios.existsById("211234563")).thenReturn(false);
        when(usuarios.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(false);
        when(encoder.encode("ana123")).thenReturn("$2a$hash-de-prueba");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = service.crear(pacienteAna());

        assertThat(resp.tipoUsuario()).isEqualTo("Paciente");
        verify(usuarios).save(argThat(u -> u.getContrasena().equals("$2a$hash-de-prueba")));
    }
}
