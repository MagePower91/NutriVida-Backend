//Publica los endpoints /api/v1/auth/....

package cl.nutrivida.identidad.controller;

import cl.nutrivida.identidad.dto.AuthDtos.AuthResponse;
import cl.nutrivida.identidad.dto.AuthDtos.LoginRequest;
import cl.nutrivida.identidad.dto.AuthDtos.RefreshRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, renovación de token y sesión")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @Operation(summary = "Usuario de la sesión actual", description = "Requiere token. Sirve para recuperar la sesión al recargar la página.")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }
}