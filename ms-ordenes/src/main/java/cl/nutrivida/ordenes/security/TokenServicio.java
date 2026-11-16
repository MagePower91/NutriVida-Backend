// Token propio de ms-ordenes para llamar a otros microservicios (por ejemplo, descontar cupos en ms-catalogo).
// Antes se reenviaba el token del paciente, y por eso cualquier paciente podía llamar a ese endpoint.
// Este token dura 1 minuto, va firmado con la misma clave que el resto y lleva el rol SERVICIO,
// que ningún usuario puede tener (no existe en ms-identidad).

package cl.nutrivida.ordenes.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class TokenServicio {

    public static final String ROL_SERVICIO = "SERVICIO";
    private static final long DURACION_SEGUNDOS = 60;

    private final JwtEncoder encoder;

    public TokenServicio(@Value("${app.jwt.secreto}") String secreto) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
    }

    public String emitir() {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nutrivida-ordenes")
                .subject("ms-ordenes")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS))
                .claim("rol", ROL_SERVICIO)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
