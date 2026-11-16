// Reglas clínicas puras (sin base de datos ni Spring), iguales a las del frontend (src/domain/clinico.js).

package cl.nutrivida.clinico.service;

import cl.nutrivida.clinico.model.Medicion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class ReglasClinicas {

    private ReglasClinicas() { }

    // IMC = peso (kg) / talla (m)², con un decimal.
    public static BigDecimal calcularImc(BigDecimal peso, Integer tallaCm) {
        if (peso == null || tallaCm == null || tallaCm <= 0 || peso.signum() <= 0) return null;
        BigDecimal metros = BigDecimal.valueOf(tallaCm).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return peso.divide(metros.multiply(metros), 1, RoundingMode.HALF_UP);
    }

    // Clasificación de la OMS para adultos.
    public static String clasificarImc(BigDecimal imc) {
        if (imc == null) return "";
        if (imc.compareTo(new BigDecimal("18.5")) < 0) return "Bajo peso";
        if (imc.compareTo(new BigDecimal("25")) < 0) return "Normal";
        if (imc.compareTo(new BigDecimal("30")) < 0) return "Sobrepeso";
        return "Obesidad";
    }

    /** Variación entre el primer y el último control (las mediciones vienen ordenadas por fecha). */
    public record Progreso(int controles, BigDecimal pesoInicial, BigDecimal pesoActual, BigDecimal variacionPeso,
                           BigDecimal variacionCintura, BigDecimal imcActual, String clasificacionImcActual) { }

    public static Progreso resumirProgreso(List<Medicion> ordenadas) {
        if (ordenadas.isEmpty()) return null;
        Medicion primera = ordenadas.get(0);
        Medicion ultima = ordenadas.get(ordenadas.size() - 1);
        BigDecimal variacionCintura = primera.getCintura() != null && ultima.getCintura() != null
                ? ultima.getCintura().subtract(primera.getCintura()) : null;
        BigDecimal imc = calcularImc(ultima.getPeso(), ultima.getTalla());
        return new Progreso(ordenadas.size(), primera.getPeso(), ultima.getPeso(),
                ultima.getPeso().subtract(primera.getPeso()), variacionCintura, imc, clasificarImc(imc));
    }
}
