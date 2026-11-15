package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.client.CatalogoClient;
import cl.nutrivida.ms.citas.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ms.citas.dto.CitaRequest;
import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.exception.HorarioNoLaboralException;
import cl.nutrivida.ms.citas.exception.HorarioOcupadoException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPago;
import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock CitaRepository citas;
    @Mock NutricionistaRepository nutricionistas;
    @Mock CatalogoClient catalogo;
    @InjectMocks CitaService servicio;

    private Nutricionista carolina;
    // Próximo miércoles a las 10:00: Carolina atiende lunes, miércoles y viernes de 09:00 a 17:00.
    private final LocalDateTime miercolesDiezAm = LocalDate.now()
            .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atTime(10, 0);

    @BeforeEach
    void preparar() {
        carolina = new Nutricionista();
        carolina.setId("NUT001");
        carolina.setNombre("Carolina Fuentes");
        carolina.setCorreo("carolina.fuentes@nutrivida.cl");
        carolina.setDesde("09:00");
        carolina.setHasta("17:00");
        carolina.setDias(List.of(1, 3, 5));
    }

    private static Jwt token(String rol, String correo) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("11111111-1").claim("rol", rol).claim("correo", correo).build();
    }

    private static Cita citaDe(String pacienteCorreo, String nutricionista) {
        Cita cita = new Cita();
        cita.setId(7L);
        cita.setPacienteCorreo(pacienteCorreo);
        cita.setNutricionista(nutricionista);
        return cita;
    }

    @Test
    void elPacienteAgendaASuNombreConPrecioDelCatalogoYQuedaPendiente() {
        when(catalogo.obtenerServicio("CN001"))
                .thenReturn(new ServicioCatalogo("CN001", "Primera consulta nutricional", "Consulta", 35000, true));
        when(nutricionistas.findAll()).thenReturn(List.of(carolina));
        when(citas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Aunque intente agendar a nombre de otra persona, el correo sale del token.
        CitaRequest datos = new CitaRequest("CN001", "Carolina Fuentes", miercolesDiezAm, "Bajar de peso", null, "otra@gmail.com");
        Cita cita = servicio.crearCita(datos, token("PACIENTE", "paciente@gmail.com"));

        assertThat(cita.getPacienteCorreo()).isEqualTo("paciente@gmail.com");
        assertThat(cita.getPrecio()).isEqualByComparingTo(BigDecimal.valueOf(35000));
        assertThat(cita.getNombreServicio()).isEqualTo("Primera consulta nutricional");
        assertThat(cita.getEstadoCita()).isEqualTo(EstadoCita.POR_CONFIRMAR);
        assertThat(cita.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(cita.getMontoPagado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void noAgendaFueraDeLaJornadaNiEnUnHorarioOcupado() {
        when(catalogo.obtenerServicio("CN001"))
                .thenReturn(new ServicioCatalogo("CN001", "Primera consulta nutricional", "Consulta", 35000, true));
        when(nutricionistas.findAll()).thenReturn(List.of(carolina));
        Jwt paciente = token("PACIENTE", "paciente@gmail.com");

        LocalDateTime martes = miercolesDiezAm.minusDays(1); // Carolina no atiende los martes
        assertThatThrownBy(() -> servicio.crearCita(new CitaRequest("CN001", "Carolina Fuentes", martes, null, null, null), paciente))
                .isInstanceOf(HorarioNoLaboralException.class);

        Cita existente = citaDe("otro@gmail.com", "Carolina Fuentes");
        when(citas.findByNutricionistaAndFechaHoraAndEstadoCitaIn(any(), any(), any())).thenReturn(List.of(existente));
        assertThatThrownBy(() -> servicio.crearCita(new CitaRequest("CN001", "Carolina Fuentes", miercolesDiezAm, null, null, null), paciente))
                .isInstanceOf(HorarioOcupadoException.class);
        verify(citas, never()).save(any());
    }

    @Test
    void unPacienteNoPuedeVerLaCitaDeOtro() {
        when(citas.findById(7L)).thenReturn(Optional.of(citaDe("otro@gmail.com", "Carolina Fuentes")));

        assertThatThrownBy(() -> servicio.buscarPorId(7L, token("PACIENTE", "paciente@gmail.com")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void elNutricionistaVeSoloSuAgendaYElPersonalVeTodo() {
        when(nutricionistas.findByCorreoIgnoreCase("carolina.fuentes@nutrivida.cl")).thenReturn(Optional.of(carolina));
        List<Cita> agenda = List.of(citaDe("paciente@gmail.com", "Carolina Fuentes"));
        when(citas.findByNutricionistaOrderByFechaHoraAsc("Carolina Fuentes")).thenReturn(agenda);

        assertThat(servicio.listarCitas(token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl"))).isEqualTo(agenda);

        servicio.listarCitas(token("RECEPCION", "recepcion@gmail.com"));
        verify(citas).findAll();
    }

    @Test
    void unNutricionistaNoPuedeAgendarCitas() {
        CitaRequest datos = new CitaRequest("CN001", null, miercolesDiezAm, null, null, null);

        assertThatThrownBy(() -> servicio.crearCita(datos, token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(catalogo, never()).obtenerServicio(any());
    }
}
