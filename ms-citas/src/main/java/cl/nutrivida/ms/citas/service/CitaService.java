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
import cl.nutrivida.ms.citas.model.MetodoPago;
import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaService {

    // Roles tal como vienen en el claim "rol" del token de ms-identidad.
    static final String PACIENTE = "PACIENTE";
    static final String NUTRICIONISTA = "NUTRICIONISTA";
    static final String ADMINISTRADOR = "ADMINISTRADOR";
    static final String RECEPCION = "RECEPCION";

    private final CitaRepository citaRepository;
    private final NutricionistaRepository nutricionistaRepository;
    private final CatalogoClient catalogoClient;

    public CitaService(CitaRepository citaRepository,
                       NutricionistaRepository nutricionistaRepository,
                       CatalogoClient catalogoClient) {
        this.citaRepository = citaRepository;
        this.nutricionistaRepository = nutricionistaRepository;
        this.catalogoClient = catalogoClient;
    }

    // Cada rol ve solo lo que le corresponde: el paciente sus citas, el nutricionista su agenda
    // y administración o recepción todas.
    @Transactional(readOnly = true)
    public List<Cita> listarCitas(Jwt usuario) {
        return switch (rolDe(usuario)) {
            case ADMINISTRADOR, RECEPCION -> citaRepository.findAll();
            case NUTRICIONISTA -> citaRepository.findByNutricionistaOrderByFechaHoraAsc(nombreDelNutricionista(usuario));
            default -> citaRepository.findByPacienteCorreoIgnoreCaseOrderByFechaHoraAsc(correoDe(usuario));
        };
    }

    @Transactional(readOnly = true)
    public Cita buscarPorId(Long id, Jwt usuario) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La cita no existe."));
        if (!puedeVer(cita, usuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta cita.");
        }
        return cita;
    }

    // Agenda una cita. El paciente siempre agenda para sí mismo (su correo sale del token);
    // administración y recepción pueden agendar a nombre de un paciente.
    // El nombre y el precio salen de ms-catalogo, y la cita nace por confirmar y con pago pendiente:
    // pagarla es tarea de ms-ordenes.
    @Transactional
    public Cita crearCita(CitaRequest datos, Jwt usuario) {
        String rol = rolDe(usuario);
        String pacienteCorreo = switch (rol) {
            case PACIENTE -> correoDe(usuario);
            case ADMINISTRADOR, RECEPCION -> {
                if (datos.pacienteCorreo() == null || datos.pacienteCorreo().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Indica el correo del paciente.");
                }
                yield datos.pacienteCorreo().trim().toLowerCase();
            }
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "Tu rol no puede agendar citas.");
        };

        ServicioCatalogo servicio = catalogoClient.obtenerServicio(datos.codigoServicio());
        if (!servicio.activo()) {
            throw new ApiException(HttpStatus.CONFLICT, "El servicio «" + servicio.nombre() + "» no está disponible.");
        }

        Cita cita = new Cita();
        cita.setPacienteCorreo(pacienteCorreo);
        cita.setCorreoConfirmacion(datos.correoConfirmacion() != null ? datos.correoConfirmacion() : pacienteCorreo);
        cita.setCodigoServicio(servicio.codigo());
        cita.setNombreServicio(servicio.nombre());
        boolean conPreferencia = datos.nutricionista() != null && !datos.nutricionista().isBlank();
        cita.setNutricionista(conPreferencia ? datos.nutricionista().trim() : null);
        cita.setNutricionistaPreferido(conPreferencia);
        cita.setMotivo(datos.motivo());
        cita.setFechaHora(datos.fechaHora());
        cita.setMetodoPago(MetodoPago.CLINICA);
        cita.setEstadoCita(EstadoCita.POR_CONFIRMAR);
        cita.setEstadoPago(EstadoPago.PENDIENTE);
        cita.setPrecio(BigDecimal.valueOf(servicio.precioFinal()));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setMontoReembolsado(BigDecimal.ZERO);
        cita.setCreadaEn(LocalDateTime.now());
        cita.setReprogramaciones(0);

        validarDisponibilidad(cita);
        return citaRepository.save(cita);
    }

    // Solo administración (se valida en el controlador). Cancelar es otra operación: esto borra el registro.
    @Transactional
    public void eliminarCita(Long id) {
        if (!citaRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La cita no existe.");
        }
        citaRepository.deleteById(id);
    }

    // Reglas de Carolina: el nutricionista debe atender ese día y hora, y no tener otra cita activa a la misma hora.
    private void validarDisponibilidad(Cita cita) {
        if (cita.getNutricionista() == null || cita.getFechaHora() == null) return;
        validarHorarioLaboral(cita);

        List<Cita> conflictos = citaRepository.findByNutricionistaAndFechaHoraAndEstadoCitaIn(
                cita.getNutricionista(),
                cita.getFechaHora(),
                List.of(EstadoCita.POR_CONFIRMAR, EstadoCita.CONFIRMADA));
        boolean horarioOcupado = conflictos.stream()
                .anyMatch(existente -> !existente.getId().equals(cita.getId()));
        if (horarioOcupado) {
            throw new HorarioOcupadoException("El nutricionista ya tiene una cita en ese horario.");
        }
    }

    private void validarHorarioLaboral(Cita cita) {
        Nutricionista nutricionista = nutricionistaRepository.findAll().stream()
                .filter(candidato -> candidato.getNombre().equalsIgnoreCase(cita.getNutricionista())
                        || candidato.getId().equalsIgnoreCase(cita.getNutricionista()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el nutricionista."));

        // Día de la semana: lunes = 1, domingo = 7
        int dia = cita.getFechaHora().getDayOfWeek().getValue();
        LocalTime hora = cita.getFechaHora().toLocalTime();
        LocalTime desde = LocalTime.parse(nutricionista.getDesde());
        LocalTime hasta = LocalTime.parse(nutricionista.getHasta());

        if (!nutricionista.getDias().contains(dia)
                || hora.isBefore(desde)
                || !hora.plusHours(1).isAfter(hora)
                || hora.plusHours(1).isAfter(hasta)) {
            throw new HorarioNoLaboralException("El nutricionista no atiende en el día u horario solicitado.");
        }
    }

    private boolean puedeVer(Cita cita, Jwt usuario) {
        return switch (rolDe(usuario)) {
            case ADMINISTRADOR, RECEPCION -> true;
            case NUTRICIONISTA -> nombreDelNutricionista(usuario).equalsIgnoreCase(String.valueOf(cita.getNutricionista()));
            default -> cita.getPacienteCorreo().equalsIgnoreCase(correoDe(usuario));
        };
    }

    private String nombreDelNutricionista(Jwt usuario) {
        return nutricionistaRepository.findByCorreoIgnoreCase(correoDe(usuario))
                .map(Nutricionista::getNombre)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Tu cuenta no está asociada a un nutricionista de la clínica."));
    }

    private static String rolDe(Jwt usuario) {
        String rol = usuario.getClaimAsString("rol");
        return rol == null ? "" : rol;
    }

    private static String correoDe(Jwt usuario) {
        return usuario.getClaimAsString("correo");
    }
}
