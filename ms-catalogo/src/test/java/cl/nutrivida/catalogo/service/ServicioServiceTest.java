package cl.nutrivida.catalogo.service;

import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioResponse;
import cl.nutrivida.catalogo.exception.ApiException;
import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.model.Servicio;
import cl.nutrivida.catalogo.repository.CategoriaRepository;
import cl.nutrivida.catalogo.repository.ServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas de servicios: cupos de talleres y validaciones al crear.
 */
@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock ServicioRepository servicios;
    @Mock CategoriaRepository categorias;
    @InjectMocks ServicioService service;

    private Servicio taller(int cupos) {
        Servicio s = new Servicio();
        s.setCodigo("TG002");
        s.setNombre("Taller de cocina nutritiva");
        s.setCategoria(new Categoria("Taller grupal", null));
        s.setPrecio(20000);
        s.setDescuento(0);
        s.setCupos(cupos);
        s.setActivo(true);
        return s;
    }

    // Verifica que al reservar cupos se descuenten de los disponibles (8 - 3 = 5).
    @Test
    @DisplayName("Reservar cupos descuenta los disponibles")
    void reservaCupos() {
        when(servicios.buscarParaActualizar("TG002")).thenReturn(Optional.of(taller(8)));

        ServicioResponse r = service.reservarCupos("TG002", 3);

        assertThat(r.cupos()).isEqualTo(5);
    }

    // Verifica que no se puedan reservar más cupos de los que quedan.
    @Test
    @DisplayName("No permite reservar más cupos de los disponibles")
    void noReservaMasDeLosDisponibles() {
        when(servicios.buscarParaActualizar("TG002")).thenReturn(Optional.of(taller(2)));

        assertThatThrownBy(() -> service.reservarCupos("TG002", 3))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No quedan cupos");
    }

    // Verifica que un taller grupal no se pueda crear sin indicar sus cupos.
    @Test
    @DisplayName("Un taller grupal sin cupos no se puede crear")
    void tallerSinCupos() {
        when(servicios.existsById("TG004")).thenReturn(false);
        when(categorias.findById(4L)).thenReturn(Optional.of(new Categoria("Taller grupal", null)));
        ServicioRequest req = new ServicioRequest("TG004", "Taller de colaciones", 4L, "90 min",
                "Presencial (grupo)", null, 15000, 0, null, null, null, null);

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cupos");
        verify(servicios, never()).save(any());
    }
}
