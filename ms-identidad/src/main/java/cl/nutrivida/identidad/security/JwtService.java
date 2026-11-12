//Crea el accessToken con el RUN, el correo y el rol del usuario.

package cl.nutrivida.identidad.security;

import cl.nutrivida.identidad.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long accessMinutos;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.access-minutos}") long accessMinutos) {
        this.encoder = encoder;
        this.accessMinutos = accessMinutos;
    }

    public String generarAccessToken(Usuario u) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nutrivida-identidad")
                .subject(u.getRun())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(accessMinutos, ChronoUnit.MINUTES))
                .claim("correo", u.getCorreo())
                .claim("nombre", u.getNombre())
                .claim("rol", u.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getAccessSegundos() { return accessMinutos * 60; }
}