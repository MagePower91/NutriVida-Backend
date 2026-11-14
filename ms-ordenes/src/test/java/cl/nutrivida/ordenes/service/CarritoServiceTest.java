package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ordenes.dto.OrdenesDtos.AgregarItemRequest;
import cl.nutrivida.ordenes.dto.OrdenesDtos.ItemResponse;
import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.model.ItemCarrito;
import cl.nutrivida.ordenes.repository.ItemCarritoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas del carrito. CatalogoClient se simula con Mockito,
 * así que no se necesita ms-catalogo encendido.
 */
@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final String RUN = "195556660";
    private final LocalDateTime manana = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);

    @Mock ItemCarritoRepository items;
    @Mock CatalogoClient catalogo;
    @InjectMocks CarritoService service;

    private ServicioCatalogo servicio(String codigo, int precioFinal, boolean activo) {
        return new ServicioCatalogo(codigo, "Servicio " + codigo, 1L, "Consulta", "30 min", "Presencial",
                "Nutricionista", precioFinal, 0, precioFinal, null, null, "desc", activo);
    }

    // Verifica que el carrito no acepte más de 10 atenciones (y que ni siquiera consulte el catálogo).
    @Test
    @DisplayName("El carrito admite como máximo 10 atenciones")
    void maximoDiezAtenciones() {
        when(items.countByRunPaciente(RUN)).thenReturn(10L);

        assertThatThrownBy(() -> service.agregar(RUN, new AgregarItemRequest("CN001", manana, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("máximo");
        verifyNoInteractions(catalogo);
    }

    // Verifica que no se puedan tener dos atenciones a la misma fecha y hora.
    @Test
    @DisplayName("No permite dos atenciones a la misma hora")
    void choqueDeHorario() {
        when(items.countByRunPaciente(RUN)).thenReturn(1L);
        when(items.existsByRunPacienteAndFechaHora(RUN, manana)).thenReturn(true);

        assertThatThrownBy(() -> service.agregar(RUN, new AgregarItemRequest("CN001", manana, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("misma hora");
    }

    // Verifica que el precio del ítem lo entregue ms-catalogo (con descuento) y no el navegador.
    @Test
    @DisplayName("El precio lo pone ms-catalogo, no el navegador")
    void usaPrecioDelCatalogo() {
        when(items.countByRunPaciente(RUN)).thenReturn(0L);
        when(items.existsByRunPacienteAndFechaHora(RUN, manana)).thenReturn(false);
        when(catalogo.obtenerServicio("EV001")).thenReturn(servicio("EV001", 15300, true));
        when(items.save(any(ItemCarrito.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse r = service.agregar(RUN, new AgregarItemRequest("ev001", manana, null));

        assertThat(r.codigoServicio()).isEqualTo("EV001");
        assertThat(r.precio()).isEqualTo(15300);
    }

    // Verifica que no se pueda agregar un servicio que el administrador desactivó.
    @Test
    @DisplayName("No permite agregar un servicio desactivado")
    void servicioInactivo() {
        when(items.countByRunPaciente(RUN)).thenReturn(0L);
        when(items.existsByRunPacienteAndFechaHora(RUN, manana)).thenReturn(false);
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 35000, false));

        assertThatThrownBy(() -> service.agregar(RUN, new AgregarItemRequest("CN001", manana, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no está disponible");
        verify(items, never()).save(any());
    }
}
