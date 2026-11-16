// Reloj de la clínica (hora de Chile), inyectable para que las pruebas puedan fijar la fecha.

package cl.nutrivida.clinico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }
}
