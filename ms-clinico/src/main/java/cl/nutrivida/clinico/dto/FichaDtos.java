// Datos que entran y salen de ms-clinico. Las entidades nunca se exponen directamente.

package cl.nutrivida.clinico.dto;

import cl.nutrivida.clinico.model.FichaClinica;
import cl.nutrivida.clinico.model.Medicion;
import cl.nutrivida.clinico.service.ReglasClinicas;
import cl.nutrivida.clinico.service.ReglasClinicas.Progreso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FichaDtos {

    private FichaDtos() { }

    // ---------- Entrada ----------

    public record CrearFichaRequest(
            @Schema(example = "paciente@gmail.com")
            @NotBlank(message = "El correo del paciente es obligatorio.")
            @Email(message = "Ingresa un correo válido.") @Size(max = 100)
            String pacienteCorreo,
            @Size(max = 500) String objetivo,
            @Size(max = 1000) String antecedentes) { }

    public record ActualizarFichaRequest(
            @Size(max = 500) String objetivo,
            @Size(max = 1000) String antecedentes,
            @Schema(description = "Plan alimenticio que ve el paciente") @Size(max = 4000) String planAlimenticio) { }

    // Mismos rangos que el frontend (src/domain/clinico.js).
    public record MedicionRequest(
            @Schema(example = "2026-11-16")
            @NotNull(message = "La fecha es obligatoria.")
            @PastOrPresent(message = "La fecha no puede ser futura.")
            LocalDate fecha,
            @Schema(example = "75.6")
            @NotNull(message = "El peso es obligatorio.")
            @DecimalMin(value = "2", message = "Ingresa un peso entre 2 y 400 kg.")
            @DecimalMax(value = "400", message = "Ingresa un peso entre 2 y 400 kg.")
            BigDecimal peso,
            @Schema(example = "165")
            @NotNull(message = "La talla es obligatoria.")
            @Min(value = 40, message = "Ingresa una talla entre 40 y 250 cm.")
            @Max(value = 250, message = "Ingresa una talla entre 40 y 250 cm.")
            Integer talla,
            @Schema(example = "88")
            @DecimalMin(value = "30", message = "Ingresa una cintura entre 30 y 250 cm, o déjala vacía.")
            @DecimalMax(value = "250", message = "Ingresa una cintura entre 30 y 250 cm, o déjala vacía.")
            BigDecimal cintura,
            @Size(max = 300) String nota) { }

    // ---------- Salida ----------

    public record MedicionResponse(Long id, LocalDate fecha, BigDecimal peso, Integer talla, BigDecimal cintura,
                                   BigDecimal imc, String clasificacionImc, String nota, String registradaPor) {
        public static MedicionResponse de(Medicion medicion) {
            BigDecimal imc = ReglasClinicas.calcularImc(medicion.getPeso(), medicion.getTalla());
            return new MedicionResponse(medicion.getId(), medicion.getFecha(), medicion.getPeso(), medicion.getTalla(),
                    medicion.getCintura(), imc, ReglasClinicas.clasificarImc(imc), medicion.getNota(), medicion.getRegistradaPor());
        }
    }

    public record FichaResponse(String pacienteCorreo, String nutricionistaCorreo, String nutricionistaNombre,
                                String objetivo, String antecedentes, String planAlimenticio, LocalDateTime planActualizadoEn,
                                List<MedicionResponse> mediciones, Progreso progreso) {
        public static FichaResponse de(FichaClinica ficha) {
            return new FichaResponse(ficha.getPacienteCorreo(), ficha.getNutricionistaCorreo(), ficha.getNutricionistaNombre(),
                    ficha.getObjetivo(), ficha.getAntecedentes(), ficha.getPlanAlimenticio(), ficha.getPlanActualizadoEn(),
                    ficha.getMediciones().stream().map(MedicionResponse::de).toList(),
                    ReglasClinicas.resumirProgreso(ficha.getMediciones()));
        }
    }

    /** Fila de la lista "Mis pacientes" del nutricionista. */
    public record PacienteResumen(String pacienteCorreo, String objetivo, int controles, LocalDate ultimoControl,
                                  BigDecimal imcActual, String clasificacionImcActual) {
        public static PacienteResumen de(FichaClinica ficha) {
            Progreso progreso = ReglasClinicas.resumirProgreso(ficha.getMediciones());
            List<Medicion> mediciones = ficha.getMediciones();
            return new PacienteResumen(ficha.getPacienteCorreo(), ficha.getObjetivo(), mediciones.size(),
                    mediciones.isEmpty() ? null : mediciones.get(mediciones.size() - 1).getFecha(),
                    progreso == null ? null : progreso.imcActual(),
                    progreso == null ? "" : progreso.clasificacionImcActual());
        }
    }
}
