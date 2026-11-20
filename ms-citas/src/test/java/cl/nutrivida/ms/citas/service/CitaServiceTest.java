package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.client.CatalogoClient;
import cl.nutrivida.ms.citas.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ms.citas.dto.AsistenciaRequest;
import cl.nutrivida.ms.citas.dto.BloqueHorario;
import cl.nutrivida.ms.citas.dto.CancelacionResponse;
import cl.nutrivida.ms.citas.dto.CitaRequest;
import cl.nutrivida.ms.citas.dto.ReprogramarRequest;
import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.exception.HorarioNoLaboralException;
import cl.nutrivida.ms.citas.exception.HorarioOcupadoException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPago;
import cl.nutrivida.ms.citas.model.MetodoPago;
import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitaServiceTest {

    private static final ZoneId CHILE = ZoneId.of("America/Santiago");
    // "Ahora" en todas las pruebas: lunes 16 de noviembre de 2026, 08:00.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 8, 0);
    private static final LocalDateTime MIERCOLES_10 = LocalDateTime.of(2026, 11, 18, 10, 0);

    @Mock CitaRepository citas;
    @Mock NutricionistaRepository nutricionistas;
    @Mock CatalogoClient catalogo;
    private CitaService servicio;

    private Nutricionista carolina;   // lunes, miércoles y viernes de 09:00 a 17:00
    private Nutricionista daniela;    // lunes a viernes de 08:00 a 13:00

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.atZone(CHILE).toInstant(), CHILE);
        servicio = new CitaService(citas, nutricionistas, catalogo, reloj);
        carolina = nutricionista("NUT001", "Carolina Fuentes", "carolina.fuentes@nutrivida.cl", "09:00", "17:00", List.of(1, 3, 5));
        daniela = nutricionista("NUT003", "Daniela Morales", "daniela.morales@nutrivida.cl", "08:00", "13:00", List.of(1, 2, 3, 4, 5));
        when(nutricionistas.findAll()).thenReturn(List.of(carolina, daniela));
        when(catalogo.obtenerServicio("CN001"))
               .thenReturn(new ServicioCatalogo(
        "CN001", "Primera consulta nutricional", "Consulta", 35000, null,true, null
        ));
        when(citas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Nutricionista nutricionista(String id, String nombre, String correo, String desde, String hasta, List<Integer> dias) {
        Nutricionista profesional = new Nutricionista();
        profesional.setId(id);
        profesional.setNombre(nombre);
        profesional.setCorreo(correo);
        profesional.setDesde(desde);
        profesional.setHasta(hasta);
        profesional.setDias(dias);
        return profesional;
    }

    private static Jwt token(String rol, String correo) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("11111111-1").claim("rol", rol).claim("correo", correo).build();
    }

    private static final Jwt PACIENTE = token("PACIENTE", "paciente@gmail.com");
    private static final Jwt RECEPCION = token("RECEPCION", "recepcion@gmail.com");

    private static Cita cita(Long id, String pacienteCorreo, String nutricionista, LocalDateTime fechaHora, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setPacienteCorreo(pacienteCorreo);
        cita.setNutricionista(nutricionista);
        cita.setCodigoServicio("CN001");
        cita.setFechaHora(fechaHora);
        cita.setEstadoCita(estado);
        cita.setEstadoPago(EstadoPago.PENDIENTE);
        cita.setMetodoPago(MetodoPago.CLINICA);
        cita.setReprogramaciones(0);
        return cita;
    }

    private static HttpStatus estadoDe(Throwable error) {
        return ((ApiException) error).getStatus();
    }

    // ---------- Agendar ----------

    @Test
    void elPacienteAgendaASuNombreConPrecioDelCatalogoYQuedaPorConfirmar() {
        // Aunque intente agendar a nombre de otra persona, el correo sale del token.
        CitaRequest datos = new CitaRequest("CN001", "Carolina Fuentes", MIERCOLES_10, "Bajar de peso", null, "otra@gmail.com");
        Cita creada = servicio.crearCita(datos, PACIENTE);

        assertThat(creada.getPacienteCorreo()).isEqualTo("paciente@gmail.com");
        assertThat(creada.getPrecio()).isEqualByComparingTo(BigDecimal.valueOf(35000));
        assertThat(creada.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        // Faltan más de 24 h: queda por confirmar hasta 24 h antes (RN-02).
        assertThat(creada.getEstadoCita()).isEqualTo(EstadoCita.POR_CONFIRMAR);
        assertThat(creada.getLimiteConfirmacion()).isEqualTo(MIERCOLES_10.minusHours(24));
        assertThat(creada.getNutricionista()).isEqualTo("Carolina Fuentes");
        assertThat(creada.isNutricionistaPreferido()).isTrue();
    }

    @Test
    void conMenosDe24HorasQuedaConfirmadaYConMenosDe2NoSePuedeAgendar() {
        Cita manana = servicio.crearCita(new CitaRequest("CN001", null, AHORA.plusHours(5), null, null, null), PACIENTE);
        assertThat(manana.getEstadoCita()).isEqualTo(EstadoCita.CONFIRMADA); // RN-07
        assertThat(manana.getLimiteConfirmacion()).isNull();

        assertThatThrownBy(() -> servicio.crearCita(new CitaRequest("CN001", null, AHORA.plusHours(1), null, null, null), PACIENTE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("2 h"); // RN-03
    }

    @Test
    void sinPreferenciaAsignaAlPrimerNutricionistaLibre() {
        // A las 10:00 del miércoles Carolina ya está ocupada; Daniela sigue libre.
        when(citas.findByFechaHoraAndEstadoCitaIn(any(), any()))
                .thenReturn(List.of(cita(9L, "otro@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA)));

        Cita creada = servicio.crearCita(new CitaRequest("CN001", null, MIERCOLES_10, null, null, null), PACIENTE);

        assertThat(creada.getNutricionista()).isEqualTo("Daniela Morales");
        assertThat(creada.isNutricionistaPreferido()).isFalse();
    }

    @Test
    void noAgendaFueraDeLaJornadaNiEnUnHorarioOcupado() {
        LocalDateTime martes = MIERCOLES_10.minusDays(1); // Carolina no atiende los martes
        assertThatThrownBy(() -> servicio.crearCita(new CitaRequest("CN001", "Carolina Fuentes", martes, null, null, null), PACIENTE))
                .isInstanceOf(HorarioNoLaboralException.class);

        when(citas.findByFechaHoraAndEstadoCitaIn(any(), any()))
                .thenReturn(List.of(cita(9L, "otro@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA)));
        assertThatThrownBy(() -> servicio.crearCita(new CitaRequest("CN001", "NUT001", MIERCOLES_10, null, null, null), PACIENTE))
                .isInstanceOf(HorarioOcupadoException.class);
        verify(citas, never()).save(any());
    }

       @Test
        void unTallerSeLlenaPorCupos() {
        when(catalogo.obtenerServicio("TG001"))
                .thenReturn(new ServicioCatalogo(
                        "TG001", "Taller de cocina saludable",
                        "Taller grupal", 15000, 1,true, null
                ));

        Cita inscrita = cita(9L, "otro@gmail.com", null, MIERCOLES_10, EstadoCita.CONFIRMADA);
        inscrita.setCodigoServicio("TG001");

        when(citas.findByFechaHoraAndEstadoCitaIn(any(), any()))
                .thenReturn(List.of(inscrita));

        assertThatThrownBy(() -> servicio.crearCita(
                new CitaRequest("TG001", null, MIERCOLES_10, null, null, null), PACIENTE))
                .isInstanceOf(HorarioOcupadoException.class)
                .hasMessageContaining("cupos");
    }
    @Test
    void unNutricionistaNoPuedeAgendarCitas() {
        CitaRequest datos = new CitaRequest("CN001", null, MIERCOLES_10, null, null, null);

        assertThatThrownBy(() -> servicio.crearCita(datos, token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(catalogo, never()).obtenerServicio(any());
    }

    // ---------- Ver ----------

    @Test
    void unPacienteNoPuedeVerLaCitaDeOtro() {
        when(citas.findById(7L)).thenReturn(Optional.of(cita(7L, "otro@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA)));

        assertThatThrownBy(() -> servicio.buscarPorId(7L, PACIENTE))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void elNutricionistaVeSoloSuAgendaYElPersonalVeTodo() {
        when(nutricionistas.findByCorreoIgnoreCase("carolina.fuentes@nutrivida.cl")).thenReturn(Optional.of(carolina));
        List<Cita> agenda = List.of(cita(7L, "paciente@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA));
        when(citas.findByNutricionistaOrderByFechaHoraAsc("Carolina Fuentes")).thenReturn(agenda);

        assertThat(servicio.listarCitas(token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl"))).isEqualTo(agenda);

        servicio.listarCitas(RECEPCION);
        verify(citas).findAll();
    }

    // ---------- Confirmar, cancelar, reprogramar y asistencia ----------

    @Test
    void confirmarAntesDelPlazoDejaLaCitaConfirmadaYDespuesLaCancela() {
        Cita aTiempo = cita(7L, "paciente@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.POR_CONFIRMAR);
        aTiempo.setLimiteConfirmacion(MIERCOLES_10.minusHours(24));
        when(citas.findById(7L)).thenReturn(Optional.of(aTiempo));
        assertThat(servicio.confirmar(7L, PACIENTE).getEstadoCita()).isEqualTo(EstadoCita.CONFIRMADA);

        Cita vencida = cita(8L, "paciente@gmail.com", "Carolina Fuentes", AHORA.plusHours(20), EstadoCita.POR_CONFIRMAR);
        vencida.setLimiteConfirmacion(AHORA.minusHours(4));
        when(citas.findById(8L)).thenReturn(Optional.of(vencida));
        assertThat(servicio.confirmar(8L, PACIENTE).getEstadoCita())
            .isEqualTo(EstadoCita.CANCELADA); // RN-06
        verify(citas).save(vencida);
    }

    @Test
    void elPacienteCancelaHasta2HorasAntesYLaRecepcionSiempre() {
        Cita proxima = cita(7L, "paciente@gmail.com", "Daniela Morales", AHORA.plusHours(1), EstadoCita.CONFIRMADA);
        when(citas.findById(7L)).thenReturn(Optional.of(proxima));

        assertThatThrownBy(() -> servicio.cancelar(7L, PACIENTE)).hasMessageContaining("Llama a la clínica");
        assertThat(servicio.cancelar(7L, RECEPCION).cita().getEstadoCita()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void cancelarUnaCitaPagadaOnlineInformaElReembolso() {
        Cita pagada = cita(7L, "paciente@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA);
        pagada.setMetodoPago(MetodoPago.ONLINE);
        pagada.setEstadoPago(EstadoPago.PAGADO);
        pagada.setMontoPagado(BigDecimal.valueOf(35000));
        when(citas.findById(7L)).thenReturn(Optional.of(pagada));

        CancelacionResponse resultado = servicio.cancelar(7L, PACIENTE);

        assertThat(resultado.reembolsoPendiente()).isEqualByComparingTo(BigDecimal.valueOf(35000)); // RN-11
    }

    @Test
    void reprogramarConservaElPagoYGuardaElHorarioAnterior() {
        Cita pagada = cita(7L, "paciente@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA);
        pagada.setEstadoPago(EstadoPago.PAGADO);
        when(citas.findById(7L)).thenReturn(Optional.of(pagada));
        LocalDateTime viernes = MIERCOLES_10.plusDays(2);

        Cita reprogramada = servicio.reprogramar(7L, new ReprogramarRequest(viernes, "Carolina Fuentes"), PACIENTE);

        assertThat(reprogramada.getFechaHora()).isEqualTo(viernes);
        assertThat(reprogramada.getFechaHoraAnterior()).isEqualTo(MIERCOLES_10);
        assertThat(reprogramada.getReprogramaciones()).isEqualTo(1);
        assertThat(reprogramada.getEstadoCita()).isEqualTo(EstadoCita.CONFIRMADA); // pagada: sigue confirmada
        assertThatThrownBy(() -> servicio.reprogramar(7L, new ReprogramarRequest(viernes, null), token("PACIENTE", "otro@gmail.com")))
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void soloSuNutricionistaOElPersonalRegistranLaAsistencia() {
        Cita confirmada = cita(7L, "paciente@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA);
        when(citas.findById(7L)).thenReturn(Optional.of(confirmada));
        when(nutricionistas.findByCorreoIgnoreCase("daniela.morales@nutrivida.cl")).thenReturn(Optional.of(daniela));
        when(nutricionistas.findByCorreoIgnoreCase("carolina.fuentes@nutrivida.cl")).thenReturn(Optional.of(carolina));

        assertThatThrownBy(() -> servicio.registrarAsistencia(7L, new AsistenciaRequest(EstadoCita.ATENDIDA),
                token("NUTRICIONISTA", "daniela.morales@nutrivida.cl")))
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));

        Cita atendida = servicio.registrarAsistencia(7L, new AsistenciaRequest(EstadoCita.ATENDIDA),
                token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl"));
        assertThat(atendida.getEstadoCita()).isEqualTo(EstadoCita.ATENDIDA);
    }

    // ---------- Disponibilidad y tarea automática ----------

    @Test
    void laDisponibilidadMuestraLaJornadaYMarcaLoOcupadoYLoMuyProximo() {
        when(citas.findByFechaHoraBetweenAndEstadoCitaIn(any(), any(), any()))
                .thenReturn(List.of(cita(9L, "otro@gmail.com", "Carolina Fuentes", MIERCOLES_10, EstadoCita.CONFIRMADA)));

        List<BloqueHorario> miercoles = servicio.disponibilidad(LocalDate.of(2026, 11, 18), "Carolina Fuentes", null);
        assertThat(miercoles).extracting(BloqueHorario::hora).containsExactly(
                "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00");
        assertThat(miercoles.get(1)).isEqualTo(new BloqueHorario("10:00", false, "Ocupado"));

        when(citas.findByFechaHoraBetweenAndEstadoCitaIn(any(), any(), any())).thenReturn(List.of());
        List<BloqueHorario> hoy = servicio.disponibilidad(AHORA.toLocalDate(), "Daniela Morales", null);
        assertThat(hoy.get(0)).isEqualTo(new BloqueHorario("08:00", false, "Muy próximo"));
        assertThat(hoy.get(2).disponible()).isTrue(); // 10:00, dos horas después
    }

    @Test
    void laTareaAutomaticaCancelaLasCitasSinConfirmarVencidas() {
        Cita vencida = cita(8L, "paciente@gmail.com", "Carolina Fuentes", AHORA.plusHours(20), EstadoCita.POR_CONFIRMAR);
        when(citas.findByEstadoCitaAndLimiteConfirmacionBefore(EstadoCita.POR_CONFIRMAR, AHORA)).thenReturn(List.of(vencida));

        assertThat(servicio.cancelarCitasVencidas()).isEqualTo(1);
        assertThat(vencida.getEstadoCita()).isEqualTo(EstadoCita.CANCELADA);
    }
}
