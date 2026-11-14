package cl.nutrivida.catalogo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del cálculo del precio final (precio con descuento) de un servicio.
 */
class ServicioTest {

    private Servicio servicio(int precio, int descuento) {
        Servicio s = new Servicio();
        s.setPrecio(precio);
        s.setDescuento(descuento);
        return s;
    }

    // Verifica que el descuento se aplique al precio (caso real EV001: 18.000 con 15% = 15.300).
    @Test
    @DisplayName("El precio final aplica el descuento (EV001: 18.000 con 15% = 15.300)")
    void aplicaDescuento() {
        assertThat(servicio(18000, 15).getPrecioFinal()).isEqualTo(15300);
    }

    // Verifica que un servicio sin descuento mantenga su precio.
    @Test
    @DisplayName("Sin descuento el precio no cambia")
    void sinDescuento() {
        assertThat(servicio(35000, 0).getPrecioFinal()).isEqualTo(35000);
    }

    // Verifica que el precio final se redondee a pesos enteros (8.999,1 -> 8.999).
    @Test
    @DisplayName("El precio final se redondea a pesos enteros")
    void redondea() {
        assertThat(servicio(9999, 10).getPrecioFinal()).isEqualTo(8999);
    }
}
