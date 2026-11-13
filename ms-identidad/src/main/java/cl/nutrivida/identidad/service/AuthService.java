//Aquí está la lógica del login, la renovación del token y el cierre de sesión.

package cl.nutrivida.identidad.service;

import cl.nutrivida.identidad.dto.AuthDtos.AuthResponse;
import cl.nutrivida.identidad.dto.AuthDtos.LoginRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.exception.ApiException;
import cl.nutrivida.identidad.model.RefreshToken;
import cl.nutrivida.identidad.model.Usuario;
import cl.nutrivida.identidad.repository.RefreshTokenRepository;
import cl.nutrivida.identidad.repository.UsuarioRepository;
import cl.nutrivida.identidad.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final long refreshDias;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwtService,
                       @Value("${app.jwt.refresh-dias}") long refreshDias) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshDias = refreshDias;
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.findByCorreoIgnoreCase(req.correo().trim())
                .filter(x -> encoder.matches(req.contrasena(), x.getContrasena()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        if (!u.isActivo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta a la clínica.");
        }
        return emitirTokens(u);
    }

    @Transactional
    public AuthResponse refresh(String token) {
        RefreshToken rt = refreshTokens.findById(token)
                .filter(t -> t.estaVigente() && t.getUsuario().isActivo())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión expirada. Inicia sesión nuevamente."));
        rt.setRevocado(true); // cada refreshToken sirve una sola vez
        return emitirTokens(rt.getUsuario());
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.findById(token).ifPresent(t -> t.setRevocado(true));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(String run) {
        return usuarios.findById(run)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    public AuthResponse emitirTokens(Usuario u) {
        RefreshToken rt = refreshTokens.save(new RefreshToken(
                UUID.randomUUID().toString(), u, Instant.now().plus(refreshDias, ChronoUnit.DAYS)));
        return new AuthResponse(jwtService.generarAccessToken(u), rt.getToken(), "Bearer",
                jwtService.getAccessSegundos(), UsuarioResponse.de(u));
    }
}