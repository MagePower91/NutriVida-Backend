package cl.nutrivida.clinico.service;

import cl.nutrivida.clinico.dto.FichaDtos.ActualizarFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.CrearFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.FichaResponse;
import cl.nutrivida.clinico.dto.FichaDtos.MedicionRequest;
import cl.nutrivida.clinico.exception.ApiException;
import cl.nutrivida.clinico.model.FichaClinica;
import cl.nutrivida.clinico.model.Medicion;
import cl.nutrivida.clinico.repository.FichaClinicaRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FichaServiceTest {

    private static final ZoneId CHILE = ZoneId.of("America/Santiago");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock FichaClinicaRepository fichas;
    private FichaService servicio;
    private FichaClinica fichaDeJaviera;

    private static Jwt token(String rol, String correo) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("11111111-1").claim("rol", rol).claim("correo", correo).claim("nombre", "Carolina").build();
    }

    private static final Jwt JAVIERA = token("PACIENTE", "paciente@gmail.com");
    private static final Jwt OTRO_PACIENTE = token("PACIENTE", "otro@gmail.com");
    private static final Jwt CAROLINA = token("NUTRICIONISTA", "carolina.fuentes@nutrivida.cl");
    private static final Jwt RODRIGO = token("NUTRICIONISTA", "rodrigo.sepulveda@nutrivida.cl");
    private static final Jwt ADMIN = token("ADMINISTRADOR", "admin@gmail.com");

    private static HttpStatus estadoDe(Throwable error) {
        return ((ApiException) error).getStatus();
    }

    @BeforeEach
    void preparar() {
        servicio = new FichaService(fichas, Clock.fixed(AHORA.atZone(CHILE).toInstant(), CHILE));
        fichaDeJaviera = new FichaClinica("paciente@gmail.com", "carolina.fuentes@nutrivida.cl", "Carolina", AHORA.minusMonths(3));
        fichaDeJaviera.agregarMedicion(new Medicion(LocalDate.of(2026, 8, 20), new BigDecimal("78.4"), 165, new BigDecimal("92"), null, null));
        when(fichas.findByPacienteCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(fichas.findByPacienteCorreoIgnoreCase("paciente@gmail.com")).thenReturn(Optional.of(fichaDeJaviera));
        when(fichas.save(any(FichaClinica.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void laPacienteVeSuFichaYSuNutricionistaTambien() {
        assertThat(servicio.miFicha(JAVIERA).pacienteCorreo()).isEqualTo("paciente@gmail.com");
        assertThat(servicio.obtener("paciente@gmail.com", CAROLINA).mediciones()).hasSize(1);
    }

    @Test
    void otroPacienteOtroNutricionistaYElAdministradorNoVenLaFicha() {
        for (Jwt sinAcceso : new Jwt[] {OTRO_PACIENTE, RODRIGO, ADMIN}) {
            assertThatThrownBy(() -> servicio.obtener("paciente@gmail.com", sinAcceso))
                    .isInstanceOf(ApiException.class)
                    .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Test
    void elNutricionistaAsignadoRegistraMedicionesYSeCalculaElImc() {
        MedicionRequest medicion = new MedicionRequest(LocalDate.of(2026, 10, 15), new BigDecimal("75.6"), 165, new BigDecimal("88"), "Control");

        FichaResponse ficha = servicio.registrarMedicion("paciente@gmail.com", medicion, CAROLINA);

        assertThat(ficha.mediciones()).hasSize(2);
        assertThat(ficha.mediciones().get(1).imc()).isEqualByComparingTo("27.8");
        assertThat(ficha.mediciones().get(1).registradaPor()).isEqualTo("carolina.fuentes@nutrivida.cl");
        assertThat(ficha.progreso().variacionPeso()).isEqualByComparingTo("-2.8");
    }

    @Test
    void otroNutricionistaYLaPacienteNoPuedenEditarLaFicha() {
        MedicionRequest medicion = new MedicionRequest(LocalDate.of(2026, 10, 15), new BigDecimal("75.6"), 165, null, null);

        assertThatThrownBy(() -> servicio.registrarMedicion("paciente@gmail.com", medicion, RODRIGO))
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> servicio.actualizar("paciente@gmail.com", new ActualizarFichaRequest(null, null, "Otro plan"), JAVIERA))
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(fichas, never()).save(any());
    }

    @Test
    void cambiarElPlanAlimenticioRegistraCuandoSeActualizo() {
        FichaResponse ficha = servicio.actualizar("paciente@gmail.com",
                new ActualizarFichaRequest("Bajar 6 kg", "Resistencia a la insulina", "Desayuno: avena con fruta."), CAROLINA);

        assertThat(ficha.planAlimenticio()).isEqualTo("Desayuno: avena con fruta.");
        assertThat(ficha.planActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    void elNutricionistaAbreUnaFichaNuevaYNoSePuedeDuplicar() {
        when(fichas.existsByPacienteCorreoIgnoreCase("nuevo@gmail.com")).thenReturn(false);
        FichaResponse nueva = servicio.crear(new CrearFichaRequest("Nuevo@Gmail.com", "Aumentar masa muscular", null), RODRIGO);
        assertThat(nueva.pacienteCorreo()).isEqualTo("nuevo@gmail.com");
        assertThat(nueva.nutricionistaCorreo()).isEqualTo("rodrigo.sepulveda@nutrivida.cl");

        when(fichas.existsByPacienteCorreoIgnoreCase("paciente@gmail.com")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new CrearFichaRequest("paciente@gmail.com", null, null), RODRIGO))
                .satisfies(error -> assertThat(estadoDe(error)).isEqualTo(HttpStatus.CONFLICT));
    }
}
