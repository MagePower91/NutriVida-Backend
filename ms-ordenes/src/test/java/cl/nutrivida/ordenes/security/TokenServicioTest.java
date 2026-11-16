package cl.nutrivida.ordenes.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServicioTest {

    private static final String SECRETO = "clave-solo-para-pruebas-de-32-caracteres!";

    private static NimbusJwtDecoder decodificador(String secreto) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void emiteUnTokenDeServicioDeCortaDuracionQueLosDemasServiciosAceptan() {
        String token = new TokenServicio(SECRETO).emitir();

        Jwt decodificado = decodificador(SECRETO).decode(token);

        assertThat(decodificado.getClaimAsString("rol")).isEqualTo("SERVICIO");
        assertThat(decodificado.getSubject()).isEqualTo("ms-ordenes");
        assertThat(Duration.between(decodificado.getIssuedAt(), decodificado.getExpiresAt())).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        String token = new TokenServicio(SECRETO).emitir();

        assertThatThrownBy(() -> decodificador("otra-clave-distinta-de-32-caracteres-xx").decode(token))
                .isInstanceOf(JwtException.class);
    }
}
