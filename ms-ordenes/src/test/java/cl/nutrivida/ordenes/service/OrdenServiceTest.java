package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ordenes.dto.OrdenesDtos.CheckoutRequest;
import cl.nutrivida.ordenes.dto.OrdenesDtos.OrdenResponse;
import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.model.ItemCarrito;
import cl.nutrivida.ordenes.model.Orden;
import cl.nutrivida.ordenes.repository.ItemCarritoRepository;
import cl.nutrivida.ordenes.repository.OrdenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Pruebas del pago simulado (checkout) y la creación de órdenes.
 */
@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    private static final String RUN = "195556660";

    @Mock OrdenRepository ordenes;
    @Mock ItemCarritoRepository items;
    @Mock CatalogoClient catalogo;
    @InjectMocks OrdenService service;

    private Jwt jwtPaciente() {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "HS256")
                .subject(RUN)
                .claim("rol", "PACIENTE")
                .claim("correo", "paciente@gmail.com")
                .build();
    }

    private ItemCarrito item(String codigo, int precio) {
        return new ItemCarrito(RUN, codigo, "Servicio " + codigo, precio, LocalDateTime.now().plusDays(2), null);
    }

    private ServicioCatalogo servicio(String codigo, int precioFinal, boolean activo) {
        return new ServicioCatalogo(codigo, "Servicio " + codigo, 1L, "Consulta", "30 min", "Presencial",
                "Nutricionista", precioFinal, 0, precioFinal, null, null, "desc", activo);
    }

    private CheckoutRequest tarjeta(String numero, String vencimiento) {
        return new CheckoutRequest("Javiera Soto", numero, vencimiento, "123", null);
    }

    // Verifica que una tarjeta terminada en 0000 sea rechazada y que no se cree orden ni se vacíe el carrito.
    @Test
    @DisplayName("Tarjeta terminada en 0000: se rechaza y no se cobra nada")
    void tarjetaRechazada() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("CN001", 35000)));
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 35000, true));

        assertThatThrownBy(() -> service.pagar(jwtPaciente(), tarjeta("4111111111110000", "12/30")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("rechazó");
        verify(ordenes, never()).save(any());
        verify(items, never()).deleteAll(anyList());
    }

    // Verifica que una tarjeta vencida se rechace antes de revisar el carrito o el catálogo.
    @Test
    @DisplayName("Tarjeta vencida: se rechaza antes de revisar el carrito")
    void tarjetaVencida() {
        assertThatThrownBy(() -> service.pagar(jwtPaciente(), tarjeta("4111111111111111", "01/24")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("vencida");
        verifyNoInteractions(items, catalogo, ordenes);
    }

    // Verifica que si UNA atención ya no está disponible en el catálogo, no se cobre ninguna.
    @Test
    @DisplayName("Si una atención ya no está disponible, no se cobra ninguna")
    void servicioNoDisponibleNoCobra() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN))
                .thenReturn(List.of(item("CN001", 35000), item("EV001", 15300)));
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 35000, true));
        when(catalogo.obtenerServicio("EV001")).thenReturn(servicio("EV001", 15300, false));

        assertThatThrownBy(() -> service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No se realizó ningún cobro");
        verify(ordenes, never()).save(any());
    }

    // Verifica que el pago exitoso cobre el precio ACTUAL del catálogo, guarde solo los últimos 4 dígitos
    // de la tarjeta y saque del carrito las atenciones pagadas.
    @Test
    @DisplayName("Pago exitoso: cobra el precio actual del catálogo y guarda solo los últimos 4 dígitos")
    void pagoExitoso() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("CN001", 35000)));
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 30000, true)); // el precio bajó
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse r = service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30"));

        assertThat(r.total()).isEqualTo(30000);
        assertThat(r.estado()).isEqualTo("PAGADA");
        assertThat(r.tarjeta()).isEqualTo("**** 1111");
        verify(items).deleteAll(anyList());
    }
}
