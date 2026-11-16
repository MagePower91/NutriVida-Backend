// Reloj de la clínica (hora de Chile). Se inyecta en vez de llamar a LocalDateTime.now()
// para que las pruebas puedan fijar la hora y comprobar reglas como "2 h antes" o "24 h antes".

package cl.nutrivida.ms.citas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }
}
