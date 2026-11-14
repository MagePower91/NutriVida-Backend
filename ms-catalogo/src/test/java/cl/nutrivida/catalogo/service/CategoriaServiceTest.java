package cl.nutrivida.catalogo.service;

import cl.nutrivida.catalogo.exception.ApiException;
import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.repository.CategoriaRepository;
import cl.nutrivida.catalogo.repository.ServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas de categorías.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository categorias;
    @Mock ServicioRepository servicios;
    @InjectMocks CategoriaService service;

    // Verifica que no se pueda eliminar una categoría que todavía tiene servicios asociados.
    @Test
    @DisplayName("No se puede eliminar una categoría con servicios asociados")
    void noEliminaCategoriaConServicios() {
        when(categorias.findById(1L)).thenReturn(Optional.of(new Categoria("Consulta", null)));
        when(servicios.existsByCategoriaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("servicios asociados");
        verify(categorias, never()).delete(any());
    }
}
