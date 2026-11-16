package cl.nutrivida.clinico.service;

import cl.nutrivida.clinico.model.Medicion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasClinicasTest {

    @Test
    void calculaElImcConUnDecimal() {
        assertThat(ReglasClinicas.calcularImc(new BigDecimal("75.6"), 165)).isEqualByComparingTo("27.8");
        assertThat(ReglasClinicas.calcularImc(new BigDecimal("70"), 0)).isNull();
    }

    @Test
    void clasificaElImcSegunLaOms() {
        assertThat(ReglasClinicas.clasificarImc(new BigDecimal("18.4"))).isEqualTo("Bajo peso");
        assertThat(ReglasClinicas.clasificarImc(new BigDecimal("24.9"))).isEqualTo("Normal");
        assertThat(ReglasClinicas.clasificarImc(new BigDecimal("25"))).isEqualTo("Sobrepeso");
        assertThat(ReglasClinicas.clasificarImc(new BigDecimal("30"))).isEqualTo("Obesidad");
    }

    @Test
    void resumeElProgresoEntreElPrimerYElUltimoControl() {
        List<Medicion> controles = List.of(
                new Medicion(LocalDate.of(2026, 8, 20), new BigDecimal("78.4"), 165, new BigDecimal("92"), null, null),
                new Medicion(LocalDate.of(2026, 10, 15), new BigDecimal("75.6"), 165, new BigDecimal("88"), null, null));

        ReglasClinicas.Progreso progreso = ReglasClinicas.resumirProgreso(controles);

        assertThat(progreso.controles()).isEqualTo(2);
        assertThat(progreso.variacionPeso()).isEqualByComparingTo("-2.8");
        assertThat(progreso.variacionCintura()).isEqualByComparingTo("-4");
        assertThat(progreso.clasificacionImcActual()).isEqualTo("Sobrepeso");
        assertThat(ReglasClinicas.resumirProgreso(List.of())).isNull();
    }
}
