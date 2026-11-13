//Publica los endpoints /api/v1/auth/.... agrega el endpoint de registro.

package cl.nutrivida.identidad.controller;

import cl.nutrivida.identidad.dto.AuthDtos.AuthResponse;
import cl.nutrivida.identidad.dto.AuthDtos.LoginRequest;
import cl.nutrivida.identidad.dto.AuthDtos.RefreshRequest;
import cl.nutrivida.identidad.dto.AuthDtos.RegistroRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.model.Usuario;
import cl.nutrivida.identidad.service.AuthService;
import cl.nutrivida.identidad.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, login, renovación de token y sesión")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Registrarse como paciente", description = "Público. Crea la cuenta y deja la sesión iniciada.")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registro(@Valid @RequestBody RegistroRequest req) {
        Usuario nuevo = usuarioService.registrarPaciente(req);
        return authService.emitirTokens(nuevo);
    }

    @Operation(summary = "Iniciar sesión", description = "Público. Devuelve accessToken (15 min) y refreshToken (7 días).")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Renovar el accessToken", description = "Público. Usa el refreshToken; entrega un par nuevo.")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @Operation(summary = "Cerrar sesión", description = "Público. Invalida el refreshToken.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
    }

    @Operation(summary = "Usuario de la sesión actual", description = "Requiere token.")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }
}