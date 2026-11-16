// Datos de demostración (la primera vez): la paciente de prueba con su ficha asignada a Carolina Fuentes,
// tres controles y un plan alimenticio. Son los mismos datos que usa el frontend en modo local.

package cl.nutrivida.clinico.config;

import cl.nutrivida.clinico.model.FichaClinica;
import cl.nutrivida.clinico.model.Medicion;
import cl.nutrivida.clinico.repository.FichaClinicaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    private static final String CAROLINA = "carolina.fuentes@nutrivida.cl";

    @Bean
    CommandLineRunner cargarFichas(FichaClinicaRepository fichas) {
        return args -> {
            if (fichas.existsByPacienteCorreoIgnoreCase("paciente@gmail.com")) return;
            FichaClinica ficha = new FichaClinica("paciente@gmail.com", CAROLINA, "Carolina", LocalDateTime.of(2026, 8, 20, 10, 0));
            ficha.setObjetivo("Bajar 6 kg en 4 meses y mejorar hábitos de colación en el trabajo.");
            ficha.setAntecedentes("Derivada por médico general. Resistencia a la insulina. Sin alergias alimentarias.");
            ficha.setPlanAlimenticio("""
                    Desayuno: avena con fruta y yogur natural.
                    Colación: un puñado de frutos secos.
                    Almuerzo: medio plato de verduras, legumbres o pescado y una porción de arroz integral.
                    Once: pan integral con palta o quesillo.
                    Cena: ensalada con proteína magra.
                    Agua: 2 litros al día.""");
            ficha.setPlanActualizadoEn(LocalDateTime.of(2026, 10, 15, 12, 0));
            ficha.agregarMedicion(new Medicion(LocalDate.of(2026, 8, 20), new BigDecimal("78.4"), 165, new BigDecimal("92"), "Evaluación inicial.", CAROLINA));
            ficha.agregarMedicion(new Medicion(LocalDate.of(2026, 9, 17), new BigDecimal("76.9"), 165, new BigDecimal("90"), "Buena adherencia al plan.", CAROLINA));
            ficha.agregarMedicion(new Medicion(LocalDate.of(2026, 10, 15), new BigDecimal("75.6"), 165, new BigDecimal("88"), "Se ajustan las colaciones de la tarde.", CAROLINA));
            fichas.save(ficha);
        };
    }
}
