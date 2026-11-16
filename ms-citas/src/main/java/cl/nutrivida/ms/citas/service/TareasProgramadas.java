// Tareas automáticas de la agenda. Cada 15 minutos cancela las citas "Por confirmar"
// cuyo plazo de confirmación (24 h antes) ya venció (RN-06).

package cl.nutrivida.ms.citas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final CitaService citaService;

    public TareasProgramadas(CitaService citaService) {
        this.citaService = citaService;
    }

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    public void cancelarCitasSinConfirmar() {
        int canceladas = citaService.cancelarCitasVencidas();
        if (canceladas > 0) {
            log.info("Se cancelaron {} cita(s) por falta de confirmación a tiempo.", canceladas);
        }
    }
}
