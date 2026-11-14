package cl.nutrivida.identidad.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la validación del RUN chileno (dígito verificador módulo 11).
 */
class RunTest {

    // Verifica que se acepten RUN con dígito verificador correcto, incluida la K en mayúscula o minúscula.
    @Test
    @DisplayName("Acepta RUN con dígito verificador correcto, incluida la K")
    void aceptaRunValidos() {
        assertThat(Run.esValido("761920839")).isTrue();
        assertThat(Run.esValido("16222333K")).isTrue();
        assertThat(Run.esValido("16222333k")).isTrue();
    }

    // Verifica que un RUN con el dígito verificador equivocado sea rechazado.
    @Test
    @DisplayName("Rechaza un RUN con dígito verificador incorrecto")
    void rechazaDigitoVerificadorIncorrecto() {
        assertThat(Run.esValido("211234566")).isFalse();
    }

    // Verifica que se rechacen los formatos inválidos: con puntos y guion, vacío o nulo.
    @Test
    @DisplayName("Rechaza formatos con puntos o guion, vacíos o nulos")
    void rechazaFormatosInvalidos() {
        assertThat(Run.esValido("19.555.666-0")).isFalse();
        assertThat(Run.esValido("")).isFalse();
        assertThat(Run.esValido(null)).isFalse();
    }
}
