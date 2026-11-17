package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ordenes.client.CitasClient;
import cl.nutrivida.ordenes.client.CitasClient.LineaPagada;
import cl.nutrivida.ordenes.client.CitasClient.PagoRequest;
import cl.nutrivida.ordenes.client.CitasClient.PagoResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
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
    @Mock CitasClient citas;
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
        verifyNoInteractions(items, catalogo, ordenes, citas);
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

        when(citas.registrarPago(any())).thenReturn(agendado(new LineaPagada(7L, null, "CN001", "Servicio CN001",
                LocalDateTime.now().plusDays(2), "Carolina Fuentes", BigDecimal.valueOf(30000))));

        OrdenResponse r = service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30"));

        assertThat(r.total()).isEqualTo(30000);
        assertThat(r.estado()).isEqualTo("PAGADA");
        assertThat(r.tarjeta()).isEqualTo("**** 1111");
        assertThat(r.detalles()).singleElement().satisfies(d -> {
            assertThat(d.citaId()).isEqualTo(7L);
            assertThat(d.nutricionista()).isEqualTo("Carolina Fuentes");
        });
        verify(items).deleteAll(anyList());
    }

    private static PagoResponse agendado(LineaPagada... lineas) {
        BigDecimal total = List.of(lineas).stream().map(LineaPagada::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PagoResponse(List.of(lineas), total);
    }

    // Verifica que el pago envíe a ms-citas lo que se agenda: las atenciones del carrito y las citas
    // con pago en clínica elegidas, con el correo del paciente que sale del token.
    @Test
    @DisplayName("Al pagar, ms-citas agenda las atenciones y paga las citas pendientes elegidas")
    void pagoAgendaEnMsCitas() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("CN001", 35000)));
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 35000, true));
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citas.registrarPago(any())).thenReturn(agendado(
                new LineaPagada(7L, null, "CN001", "Servicio CN001", LocalDateTime.now().plusDays(2), "Carolina Fuentes", BigDecimal.valueOf(35000)),
                new LineaPagada(3L, null, "EV001", "Antropometría", LocalDateTime.now().plusDays(3), "Daniela Morales", BigDecimal.valueOf(18000))));

        OrdenResponse r = service.pagar(jwtPaciente(), new CheckoutRequest("Javiera Soto", "4111111111111111", "12/30", "123",
                null, List.of(3L), null));

        ArgumentCaptor<PagoRequest> enviado = ArgumentCaptor.forClass(PagoRequest.class);
        verify(citas).registrarPago(enviado.capture());
        assertThat(enviado.getValue().pacienteCorreo()).isEqualTo("paciente@gmail.com");
        assertThat(enviado.getValue().items()).extracting(CitasClient.ItemPagado::codigoServicio).containsExactly("CN001");
        assertThat(enviado.getValue().citaIds()).containsExactly(3L);
        assertThat(r.total()).isEqualTo(53000);
        assertThat(r.detalles()).hasSize(2);
    }

    // Verifica que si ms-citas no puede agendar una atención (por ejemplo, el horario se ocupó),
    // no se vacíe el carrito: no se cobra nada.
    @Test
    @DisplayName("Si la agenda rechaza una atención, no se cobra ni se vacía el carrito")
    void agendaRechazaNoCobra() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("CN001", 35000)));
        when(catalogo.obtenerServicio("CN001")).thenReturn(servicio("CN001", 35000, true));
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citas.registrarPago(any())).thenThrow(new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                "No se realizó ningún cobro: «Servicio CN001»: El nutricionista ya tiene una cita en ese horario."));

        assertThatThrownBy(() -> service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ya tiene una cita");
        verify(items, never()).deleteAll(anyList());
    }

    // Verifica que se pueda pagar solo una cita con pago en clínica, sin ítems del carrito.
    @Test
    @DisplayName("Se puede pagar solo una cita con pago en clínica, sin ítems del carrito")
    void pagaSoloCitaPendiente() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("CN001", 35000)));
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citas.registrarPago(any())).thenReturn(agendado(new LineaPagada(3L, null, "EV001", "Antropometría",
                LocalDateTime.now().plusDays(3), "Daniela Morales", BigDecimal.valueOf(18000))));

        OrdenResponse r = service.pagar(jwtPaciente(), new CheckoutRequest("Javiera Soto", "4111111111111111", "12/30", "123",
                List.of(), List.of(3L), null));

        assertThat(r.total()).isEqualTo(18000);
        verify(items).deleteAll(List.of());   // el ítem del carrito no se eligió: se queda
    }

    private ServicioCatalogo taller(String codigo, int cupos) {
        return new ServicioCatalogo(codigo, "Taller " + codigo, 4L, "Taller grupal", "90 min", "Presencial (grupo)",
                "Nutricionista", 15000, 0, 15000, null, cupos, "desc", true);
    }

    // Verifica que los cupos del catálogo se traten como capacidad POR HORARIO: pagar un taller no consulta
    // ni descuenta un "total" de cupos; quien decide si el horario tiene lugar es ms-citas al agendar.
    @Test
    @DisplayName("Pagar un taller no descuenta cupos del catálogo: el cupo por horario lo controla ms-citas")
    void tallerNoDescuentaCuposDelCatalogo() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("TG001", 15000), item("TG001", 15000)));
        when(catalogo.obtenerServicio("TG001")).thenReturn(taller("TG001", 1)); // 1 cupo por horario
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citas.registrarPago(any())).thenReturn(agendado(
                new LineaPagada(1L, null, "TG001", "Taller TG001", LocalDateTime.now().plusDays(2), null, BigDecimal.valueOf(15000)),
                new LineaPagada(2L, null, "TG001", "Taller TG001", LocalDateTime.now().plusDays(9), null, BigDecimal.valueOf(15000))));

        // Dos sesiones del mismo taller en días distintos: cada horario tiene su propio cupo.
        OrdenResponse r = service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30"));

        assertThat(r.total()).isEqualTo(30000);
        verify(catalogo, times(1)).obtenerServicio("TG001");
        verifyNoMoreInteractions(catalogo);
    }

    // Verifica que si el horario del taller ya está lleno, el rechazo de ms-citas evita el cobro.
    @Test
    @DisplayName("Si el horario del taller está lleno, ms-citas lo rechaza y no se cobra")
    void tallerLlenoNoCobra() {
        when(items.findByRunPacienteOrderByFechaHoraAsc(RUN)).thenReturn(List.of(item("TG001", 15000)));
        when(catalogo.obtenerServicio("TG001")).thenReturn(taller("TG001", 10));
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citas.registrarPago(any())).thenThrow(new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                "No se realizó ningún cobro: «Taller TG001»: El taller ya no tiene cupos en ese horario."));

        assertThatThrownBy(() -> service.pagar(jwtPaciente(), tarjeta("4111111111111111", "12/30")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no tiene cupos en ese horario");
        verify(items, never()).deleteAll(anyList());
    }
}
