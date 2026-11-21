package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.client.CatalogoClient;
import cl.nutrivida.ms.citas.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ms.citas.dto.PagoDtos.ItemPagado;
import cl.nutrivida.ms.citas.dto.PagoDtos.LineaPagada;
import cl.nutrivida.ms.citas.dto.PagoDtos.PagoRequest;
import cl.nutrivida.ms.citas.dto.PagoDtos.PagoResponse;
import cl.nutrivida.ms.citas.dto.PagoDtos.SesionRequest;
import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPago;
import cl.nutrivida.ms.citas.model.EstadoPlan;
import cl.nutrivida.ms.citas.model.MetodoPago;
import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.model.Plan;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import cl.nutrivida.ms.citas.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la unión entre el pago (ms-ordenes) y la agenda: citas pagadas online, planes de sesiones,
 * pago en clínica y reembolsos. Los repositorios y ms-catalogo se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoYPlanesTest {

    private static final ZoneId CHILE = ZoneId.of("America/Santiago");
    // "Ahora": lunes 16 de noviembre de 2026, 08:00. Carolina atiende lunes, miércoles y viernes de 09:00 a 17:00.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 8, 0);
    private static final LocalDateTime MIERCOLES_10 = LocalDateTime.of(2026, 11, 18, 10, 0);
    private static final String PACIENTE = "paciente@gmail.com";

    @Mock CitaRepository citas;
    @Mock PlanRepository planes;
    @Mock NutricionistaRepository nutricionistas;
    @Mock CatalogoClient catalogo;

    private CitaService citaService;
    private PagoService pagoService;
    private PlanService planService;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.atZone(CHILE).toInstant(), CHILE);
        citaService = new CitaService(citas, nutricionistas, catalogo, reloj);
        pagoService = new PagoService(citas, planes, catalogo, citaService, reloj);
        planService = new PlanService(planes, citas, pagoService, catalogo, reloj);

        Nutricionista carolina = new Nutricionista();
        carolina.setId("NUT001");
        carolina.setNombre("Carolina Fuentes");
        carolina.setCorreo("carolina.fuentes@nutrivida.cl");
        carolina.setDesde("09:00");
        carolina.setHasta("17:00");
        carolina.setDias(List.of(1, 3, 5));
        when(nutricionistas.findAll()).thenReturn(List.of(carolina));

        when(catalogo.obtenerServicio("CN001"))
                .thenReturn(new ServicioCatalogo("CN001", "Primera consulta nutricional", "Consulta", 35000, null, true));
        when(catalogo.obtenerServicio("PL001"))
                .thenReturn(new ServicioCatalogo("PL001", "Plan pérdida de peso (1 mes)", "Plan especializado", 65000, null, true, 2));
        when(citas.save(any(Cita.class))).thenAnswer(invocacion -> {
            Cita cita = invocacion.getArgument(0);
            if (cita.getId() == null) cita.setId(100L);
            return cita;
        });
        when(planes.save(any(Plan.class))).thenAnswer(invocacion -> {
            Plan plan = invocacion.getArgument(0);
            if (plan.getId() == null) plan.setId(9L);
            return plan;
        });
    }

    private static PagoRequest pago(List<ItemPagado> items, List<Long> citaIds) {
        return new PagoRequest("15", PACIENTE, null, items, citaIds);
    }

    private static Jwt token(String rol, String correo) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("195556660").claim("rol", rol).claim("correo", correo).build();
    }

    private static Cita citaClinica(Long id, String paciente, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setPacienteCorreo(paciente);
        cita.setCodigoServicio("EV001");
        cita.setNombreServicio("Antropometría completa");
        cita.setFechaHora(MIERCOLES_10.plusDays(2));
        cita.setEstadoCita(estado);
        cita.setMetodoPago(MetodoPago.CLINICA);
        cita.setEstadoPago(EstadoPago.PENDIENTE);
        cita.setPrecio(BigDecimal.valueOf(15300));
        cita.setMontoPagado(BigDecimal.ZERO);
        return cita;
    }

    private static Plan plan(int sesiones) {
        Plan plan = new Plan();
        plan.setId(9L);
        plan.setPacienteCorreo(PACIENTE);
        plan.setCodigoServicio("PL001");
        plan.setNombre("Plan pérdida de peso (1 mes)");
        plan.setSesionesTotales(sesiones);
        plan.setMontoPagado(BigDecimal.valueOf(65000));
        plan.setEstadoPlan(EstadoPlan.ACTIVO);
        return plan;
    }

    private static Cita sesion(EstadoCita estado) {
        Cita sesion = citaClinica(null, PACIENTE, estado);
        sesion.setPlanId("9");
        return sesion;
    }

    // ---------- Pago del carrito ----------

    @Test
    @DisplayName("Una consulta pagada online queda confirmada, pagada y con el precio del catálogo")
    void consultaPagadaQuedaConfirmada() {
        PagoResponse respuesta = pagoService.registrarPagoOnline(
                pago(List.of(new ItemPagado("CN001", MIERCOLES_10, "Carolina Fuentes", "Bajar de peso")), null));

        LineaPagada linea = respuesta.lineas().get(0);
        assertThat(linea.citaId()).isEqualTo(100L);
        assertThat(linea.precio()).isEqualByComparingTo("35000");
        assertThat(linea.nutricionista()).isEqualTo("Carolina Fuentes");
        assertThat(respuesta.total()).isEqualByComparingTo("35000");
        verify(citas).save(org.mockito.ArgumentMatchers.argThat(cita ->
                cita.getEstadoCita() == EstadoCita.CONFIRMADA && cita.getEstadoPago() == EstadoPago.PAGADO
                        && cita.getMetodoPago() == MetodoPago.ONLINE && "15".equals(cita.getOrdenId())));
    }

    @Test
    @DisplayName("Si el horario se ocupó, el pago se rechaza indicando qué atención cambiar")
    void horarioOcupadoRechazaElPago() {
        Cita ocupada = citaClinica(5L, "otra@gmail.com", EstadoCita.CONFIRMADA);
        ocupada.setNutricionista("Carolina Fuentes");
        ocupada.setFechaHora(MIERCOLES_10);
        when(citas.findByFechaHoraAndEstadoCitaIn(eq(MIERCOLES_10), any())).thenReturn(List.of(ocupada));

        assertThatThrownBy(() -> pagoService.registrarPagoOnline(
                pago(List.of(new ItemPagado("CN001", MIERCOLES_10, "Carolina Fuentes", null)), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Primera consulta nutricional")
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(citas, never()).save(any());
    }

    @Test
    @DisplayName("Un plan sin horario se contrata sin crear sesiones")
    void planSinHorarioSoloSeContrata() {
        PagoResponse respuesta = pagoService.registrarPagoOnline(pago(List.of(new ItemPagado("PL001", null, null, null, "C-1")), null));

        LineaPagada linea = respuesta.lineas().get(0);
        assertThat(linea.planId()).isEqualTo(9L);
        assertThat(linea.citaId()).isNull();
        assertThat(linea.precio()).isEqualByComparingTo("65000");
        verify(planes).save(org.mockito.ArgumentMatchers.argThat(plan -> plan.getSesionesTotales() == 2));
        verify(citas, never()).save(any());
    }

    @Test
    @DisplayName("Un plan con horario crea su primera sesión, sin precio propio")
    void planConHorarioCreaPrimeraSesion() {
        PagoResponse respuesta = pagoService.registrarPagoOnline(
                pago(List.of(new ItemPagado("PL001", MIERCOLES_10, "Carolina Fuentes", null, "C-2")), null));

        assertThat(respuesta.lineas().get(0).citaId()).isEqualTo(100L);
        assertThat(respuesta.total()).isEqualByComparingTo("65000");   // se cobra el plan, no la sesión
        verify(citas).save(org.mockito.ArgumentMatchers.argThat(cita ->
                "9".equals(cita.getPlanId()) && cita.getPrecio().signum() == 0 && cita.getEstadoCita() == EstadoCita.CONFIRMADA));
    }

    @Test
    @DisplayName("Una cita con pago en clínica se puede pagar online en el carrito y queda confirmada")
    void citaPendienteSePagaOnline() {
        Cita pendiente = citaClinica(3L, PACIENTE, EstadoCita.POR_CONFIRMAR);
        when(citas.findById(3L)).thenReturn(Optional.of(pendiente));

        PagoResponse respuesta = pagoService.registrarPagoOnline(pago(null, List.of(3L)));

        assertThat(respuesta.total()).isEqualByComparingTo("15300");
        assertThat(pendiente.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pendiente.getEstadoCita()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(pendiente.getMontoPagado()).isEqualByComparingTo("15300");
    }

    @Test
    @DisplayName("No se puede pagar la cita de otro paciente")
    void citaDeOtroPacienteNoSePaga() {
        when(citas.findById(3L)).thenReturn(Optional.of(citaClinica(3L, "otra@gmail.com", EstadoCita.POR_CONFIRMAR)));

        assertThatThrownBy(() -> pagoService.registrarPagoOnline(pago(null, List.of(3L))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---------- Planes ----------

    @Test
    @DisplayName("El saldo del plan descuenta atendidas y agendadas; las canceladas vuelven al saldo (RN-13)")
    void resumenDelPlan() {
        PlanService.Resumen resumen = PlanService.resumir(plan(6),
                List.of(sesion(EstadoCita.ATENDIDA), sesion(EstadoCita.CONFIRMADA), sesion(EstadoCita.CANCELADA)));

        assertThat(resumen.atendidas()).isEqualTo(1);
        assertThat(resumen.agendadas()).isEqualTo(1);
        assertThat(resumen.porAgendar()).isEqualTo(4);
    }

    @Test
    @DisplayName("El paciente agenda otra sesión de su plan sin pagar mientras le queden sesiones")
    void agendaSesionConSaldo() {
        when(planes.findById(9L)).thenReturn(Optional.of(plan(2)));
        when(citas.findByPlanId("9")).thenReturn(List.of(sesion(EstadoCita.ATENDIDA)));

        Cita nueva = planService.agendarSesion(9L, new SesionRequest(MIERCOLES_10, null), token("PACIENTE", PACIENTE));

        assertThat(nueva.getPlanId()).isEqualTo("9");
        assertThat(nueva.getPrecio()).isEqualByComparingTo("0");
        assertThat(nueva.getNutricionista()).isEqualTo("Carolina Fuentes");   // asignada por la clínica
    }

    @Test
    @DisplayName("Sin sesiones por agendar, no se puede agendar otra")
    void sinSaldoNoAgenda() {
        when(planes.findById(9L)).thenReturn(Optional.of(plan(2)));
        when(citas.findByPlanId("9")).thenReturn(List.of(sesion(EstadoCita.ATENDIDA), sesion(EstadoCita.CONFIRMADA)));

        assertThatThrownBy(() -> planService.agendarSesion(9L, new SesionRequest(MIERCOLES_10, null), token("PACIENTE", PACIENTE)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("todas las sesiones");
    }

    @Test
    @DisplayName("Un paciente no puede ver ni usar el plan de otro")
    void planDeOtroPaciente() {
        when(planes.findById(9L)).thenReturn(Optional.of(plan(2)));

        assertThatThrownBy(() -> planService.obtener(9L, token("PACIENTE", "otra@gmail.com")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---------- Pago en clínica y reembolsos ----------

    @Test
    @DisplayName("Recepción registra el pago en clínica; no se puede registrar dos veces")
    void pagoEnClinica() {
        Cita cita = citaClinica(3L, PACIENTE, EstadoCita.CONFIRMADA);
        when(citas.findById(3L)).thenReturn(Optional.of(cita));

        Cita pagada = citaService.registrarPagoEnClinica(3L);
        assertThat(pagada.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pagada.getMontoPagado()).isEqualByComparingTo("15300");

        assertThatThrownBy(() -> citaService.registrarPagoEnClinica(3L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    @DisplayName("Una cita pagada online y cancelada se reembolsa completa (RN-11); sin pago no hay reembolso")
    void reembolso() {
        Cita cancelada = citaClinica(4L, PACIENTE, EstadoCita.CANCELADA);
        cancelada.setMetodoPago(MetodoPago.ONLINE);
        cancelada.setEstadoPago(EstadoPago.PAGADO);
        cancelada.setMontoPagado(BigDecimal.valueOf(15300));
        when(citas.findById(4L)).thenReturn(Optional.of(cancelada));
        when(citas.findById(5L)).thenReturn(Optional.of(citaClinica(5L, PACIENTE, EstadoCita.CANCELADA)));

        Cita reembolsada = citaService.registrarReembolso(4L);
        assertThat(reembolsada.getEstadoPago()).isEqualTo(EstadoPago.REEMBOLSADO_TOTAL);
        assertThat(reembolsada.getMontoReembolsado()).isEqualByComparingTo("15300");

        assertThatThrownBy(() -> citaService.registrarReembolso(5L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reembolso pendiente");
    }
}
