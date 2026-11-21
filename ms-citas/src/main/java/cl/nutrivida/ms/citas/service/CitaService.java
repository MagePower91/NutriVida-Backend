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
import cl.nutrivida.ms.citas.service.ReglasCita.Evaluacion;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

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
    private final Clock reloj;

    public CitaService(CitaRepository citaRepository,
                       NutricionistaRepository nutricionistaRepository,
                       CatalogoClient catalogoClient,
                       Clock reloj) {
        this.citaRepository = citaRepository;
        this.nutricionistaRepository = nutricionistaRepository;
        this.catalogoClient = catalogoClient;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    // ---------- Consultas ----------

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
        Cita cita = obtener(id);
        if (!puedeVer(cita, usuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta cita.");
        }
        return cita;
    }

    // Bloques de una hora del día pedido: la jornada del nutricionista elegido o la de todos los que atienden.
    // Cada bloque dice si se puede reservar y, si no, por qué (muy próximo, ocupado o sin cupos).
    @Transactional(readOnly = true)
    public List<BloqueHorario> disponibilidad(LocalDate fecha, String nutricionista, String codigoServicio) {
        Nutricionista elegido = textoVacio(nutricionista) ? null : buscarNutricionista(nutricionista);
        List<Nutricionista> quienes = elegido != null ? List.of(elegido) : nutricionistaRepository.findAll();
        Integer cupos = textoVacio(codigoServicio) ? null : catalogoClient.obtenerServicio(codigoServicio).cupos();

        TreeSet<LocalTime> bloques = new TreeSet<>();
        quienes.stream().filter(profesional -> atiendeElDia(profesional, fecha))
                .forEach(profesional -> bloques.addAll(bloquesDeJornada(profesional)));

        List<Cita> delDia = citaRepository.findByFechaHoraBetweenAndEstadoCitaIn(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), ReglasCita.ESTADOS_ACTIVOS);
        List<BloqueHorario> resultado = new ArrayList<>();
        for (LocalTime hora : bloques) {
            LocalDateTime momento = fecha.atTime(hora);
            List<Cita> aEsaHora = delDia.stream().filter(cita -> cita.getFechaHora().equals(momento)).toList();
            String motivo = "";
            if (ReglasCita.horasHasta(momento, ahora()) < ReglasCita.ANTICIPACION_MINIMA_HORAS) {
                motivo = "Muy próximo";
            } else if (cupos != null) {
                long inscritos = aEsaHora.stream().filter(cita -> codigoServicio.equalsIgnoreCase(cita.getCodigoServicio())).count();
                if (inscritos >= cupos) motivo = "Sin cupos";
            } else if (libresEntre(quienes, momento, aEsaHora).isEmpty()) {
                motivo = "Ocupado";
            }
            resultado.add(new BloqueHorario(hora.toString(), motivo.isEmpty(), motivo));
        }
        return resultado;
    }

    // ---------- Agendar ----------

    // Agenda una cita con pago en clínica. El paciente siempre agenda para sí mismo (su correo sale del token);
    // administración y recepción pueden agendar a nombre de un paciente. El nombre y el precio salen de
    // ms-catalogo. Pagarla online es tarea de ms-ordenes.
    @Transactional
    public Cita crearCita(CitaRequest datos, Jwt usuario) {
        String pacienteCorreo = switch (rolDe(usuario)) {
            case PACIENTE -> correoDe(usuario);
            case ADMINISTRADOR, RECEPCION -> {
                if (textoVacio(datos.pacienteCorreo())) {
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
        Evaluacion evaluacion = ReglasCita.evaluarReserva(datos.fechaHora(), MetodoPago.CLINICA, ahora());

        Cita cita = new Cita();
        cita.setPacienteCorreo(pacienteCorreo);
        cita.setCorreoConfirmacion(datos.correoConfirmacion() != null ? datos.correoConfirmacion() : pacienteCorreo);
        cita.setCodigoServicio(servicio.codigo());
        cita.setNombreServicio(servicio.nombre());
        cita.setMotivo(datos.motivo());
        cita.setFechaHora(datos.fechaHora());
        cita.setMetodoPago(MetodoPago.CLINICA);
        cita.setEstadoCita(evaluacion.estadoCita());
        cita.setLimiteConfirmacion(evaluacion.limiteConfirmacion());
        cita.setEstadoPago(EstadoPago.PENDIENTE);
        cita.setPrecio(BigDecimal.valueOf(servicio.precioFinal()));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setMontoReembolsado(BigDecimal.ZERO);
        cita.setCreadaEn(ahora());
        cita.setReprogramaciones(0);

        asignarHorario(cita, datos.nutricionista(), servicio.cupos());
        return citaRepository.save(cita);
    }

    // ---------- Acciones sobre una cita ----------

    // RN-05: el paciente confirma su asistencia. Si el plazo ya venció, la cita se cancela (RN-06).
    @Transactional
    public Cita confirmar(Long id, Jwt usuario) {
        Cita cita = obtener(id);
        exigirPacienteDuenoOPersonal(cita, usuario);
        if (cita.getEstadoCita() != EstadoCita.POR_CONFIRMAR) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cita no está pendiente de confirmación.");
        }
        if (ReglasCita.debeCancelarsePorFaltaDeConfirmacion(cita, ahora())) {
            cita.setEstadoCita(EstadoCita.CANCELADA);
            citaRepository.save(cita);
            return cita;
        }
        cita.setEstadoCita(EstadoCita.CONFIRMADA);
        cita.setLimiteConfirmacion(null);
        return citaRepository.save(cita);
    }

    // RN-09: el paciente cancela hasta 2 h antes; con menos tiempo debe llamar y la recepción cancela.
    // RN-11: si la había pagado online, se informa el reembolso pendiente.
    @Transactional
    public CancelacionResponse cancelar(Long id, Jwt usuario) {
        Cita cita = obtener(id);
        exigirPacienteDuenoOPersonal(cita, usuario);
        if (!ReglasCita.estaActiva(cita)) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cita ya no se puede cancelar.");
        }
        if (PACIENTE.equals(rolDe(usuario)) && !ReglasCita.puedeCambiarPaciente(cita, ahora())) {
            throw new ApiException(HttpStatus.CONFLICT, "Con menos de " + ReglasCita.ANTICIPACION_MINIMA_HORAS
                    + " h de anticipación no se puede cancelar en línea. Llama a la clínica.");
        }
        cita.setEstadoCita(EstadoCita.CANCELADA);
        cita.setLimiteConfirmacion(null);
        Cita guardada = citaRepository.save(cita);
        return new CancelacionResponse(guardada, ReglasCita.reembolsoPendiente(guardada));
    }

    // RN-09: cambiar fecha, hora o nutricionista con las mismas 2 h de anticipación. La nueva hora debe
    // estar libre. Si ya estaba pagada sigue confirmada; si no, se vuelve a calcular su plazo (RN-02, RN-07).
    @Transactional
    public Cita reprogramar(Long id, ReprogramarRequest datos, Jwt usuario) {
        Cita cita = obtener(id);
        exigirPacienteDuenoOPersonal(cita, usuario);
        if (!ReglasCita.estaActiva(cita)) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se puede reprogramar una cita activa.");
        }
        if (PACIENTE.equals(rolDe(usuario)) && !ReglasCita.puedeCambiarPaciente(cita, ahora())) {
            throw new ApiException(HttpStatus.CONFLICT, "Con menos de " + ReglasCita.ANTICIPACION_MINIMA_HORAS
                    + " h de anticipación no se puede reprogramar en línea. Llama a la clínica.");
        }
        String nuevoNutricionista = textoVacio(datos.nutricionista()) ? null : buscarNutricionista(datos.nutricionista()).getNombre();
        boolean mismoHorario = datos.fechaHora().equals(cita.getFechaHora())
                && (nuevoNutricionista == null || nuevoNutricionista.equalsIgnoreCase(cita.getNutricionista()));
        if (mismoHorario) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Elige una fecha, hora o nutricionista distinto al actual.");
        }

        boolean pagada = cita.getEstadoPago() == EstadoPago.PAGADO;
        Evaluacion evaluacion = ReglasCita.evaluarReserva(datos.fechaHora(),
                pagada ? MetodoPago.ONLINE : MetodoPago.CLINICA, ahora());
        Integer cupos = catalogoClient.obtenerServicio(cita.getCodigoServicio()).cupos();

        LocalDateTime anterior = cita.getFechaHora();
        cita.setFechaHora(datos.fechaHora());
        asignarHorario(cita, datos.nutricionista(), cupos);
        cita.setFechaHoraAnterior(anterior);
        cita.setReprogramaciones((cita.getReprogramaciones() == null ? 0 : cita.getReprogramaciones()) + 1);
        if (!pagada) {
            cita.setEstadoCita(evaluacion.estadoCita());
            cita.setLimiteConfirmacion(evaluacion.limiteConfirmacion());
        }
        return citaRepository.save(cita);
    }

    // RN-10: después de la atención se marca si el paciente asistió. Lo hace su nutricionista o el personal.
    @Transactional
    public Cita registrarAsistencia(Long id, AsistenciaRequest datos, Jwt usuario) {
        Cita cita = obtener(id);
        String rol = rolDe(usuario);
        boolean esSuNutricionista = NUTRICIONISTA.equals(rol)
                && nombreDelNutricionista(usuario).equalsIgnoreCase(String.valueOf(cita.getNutricionista()));
        if (!esSuNutricionista && !ADMINISTRADOR.equals(rol) && !RECEPCION.equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el nutricionista de la cita o el personal registran la asistencia.");
        }
        if (datos.estado() != EstadoCita.ATENDIDA && datos.estado() != EstadoCita.INASISTENCIA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La asistencia solo puede ser ATENDIDA o INASISTENCIA.");
        }
        if (cita.getEstadoCita() != EstadoCita.CONFIRMADA) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se registra la asistencia de una cita confirmada.");
        }
        cita.setEstadoCita(datos.estado());
        return citaRepository.save(cita);
    }

    // RN-02: la recepción registra el pago de una cita con pago en clínica (efectivo o tarjeta en el mesón).
    // Se puede antes o después de la atención, mientras no esté cancelada.
    @Transactional
    public Cita registrarPagoEnClinica(Long id) {
        Cita cita = obtener(id);
        if (cita.getMetodoPago() != MetodoPago.CLINICA || cita.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cita no tiene un pago en clínica pendiente.");
        }
        if (cita.getEstadoCita() == EstadoCita.CANCELADA) {
            throw new ApiException(HttpStatus.CONFLICT, "No se registra el pago de una cita cancelada.");
        }
        cita.setEstadoPago(EstadoPago.PAGADO);
        cita.setMontoPagado(cita.getPrecio());
        return citaRepository.save(cita);
    }

    // RN-11: el administrador registra la devolución completa de una cita pagada online que terminó
    // cancelada o en inasistencia.
    @Transactional
    public Cita registrarReembolso(Long id) {
        Cita cita = obtener(id);
        BigDecimal pendiente = ReglasCita.reembolsoPendiente(cita);
        if (pendiente.signum() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cita no tiene un reembolso pendiente.");
        }
        cita.setMontoReembolsado(pendiente);
        cita.setEstadoPago(EstadoPago.REEMBOLSADO_TOTAL);
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

    // RN-06: cancela las citas "Por confirmar" cuyo plazo venció. La ejecuta TareasProgramadas.
    @Transactional
    public int cancelarCitasVencidas() {
        List<Cita> vencidas = citaRepository.findByEstadoCitaAndLimiteConfirmacionBefore(EstadoCita.POR_CONFIRMAR, ahora());
        vencidas.forEach(cita -> cita.setEstadoCita(EstadoCita.CANCELADA));
        citaRepository.saveAll(vencidas);
        return vencidas.size();
    }

    // ---------- Disponibilidad y asignación ----------

    // Deja la cita con su nutricionista (el preferido o el primero libre) y verifica que el horario sirva.
    // Los talleres (con cupos) no llevan nutricionista: se controla que no se pase de los cupos.
    void asignarHorario(Cita cita, String preferido, Integer cupos) {
        List<Cita> aEsaHora = citaRepository.findByFechaHoraAndEstadoCitaIn(cita.getFechaHora(), ReglasCita.ESTADOS_ACTIVOS)
                .stream().filter(otra -> !Objects.equals(otra.getId(), cita.getId())).toList();

        if (cupos != null) {
            long inscritos = aEsaHora.stream().filter(otra -> cita.getCodigoServicio().equalsIgnoreCase(otra.getCodigoServicio())).count();
            if (inscritos >= cupos) throw new HorarioOcupadoException("El taller ya no tiene cupos en ese horario.");
            cita.setNutricionista(null);
            cita.setNutricionistaPreferido(false);
            return;
        }

        if (!textoVacio(preferido)) {
            Nutricionista elegido = buscarNutricionista(preferido);
            if (!atiendeEn(elegido, cita.getFechaHora())) {
                throw new HorarioNoLaboralException("El nutricionista no atiende en el día u horario solicitado.");
            }
            if (libresEntre(List.of(elegido), cita.getFechaHora(), aEsaHora).isEmpty()) {
                throw new HorarioOcupadoException("El nutricionista ya tiene una cita en ese horario.");
            }
            cita.setNutricionista(elegido.getNombre());
            cita.setNutricionistaPreferido(true);
            return;
        }

        // Sin preferencia, la clínica asigna al primer nutricionista libre en ese bloque.
        List<Nutricionista> libres = libresEntre(nutricionistaRepository.findAll(), cita.getFechaHora(), aEsaHora);
        if (libres.isEmpty()) {
            throw new HorarioOcupadoException("No hay nutricionistas disponibles en ese horario.");
        }
        cita.setNutricionista(libres.get(0).getNombre());
        cita.setNutricionistaPreferido(false);
    }

    private List<Nutricionista> libresEntre(List<Nutricionista> candidatos, LocalDateTime momento, List<Cita> aEsaHora) {
        return candidatos.stream()
                .filter(profesional -> atiendeEn(profesional, momento))
                .filter(profesional -> aEsaHora.stream()
                        .noneMatch(cita -> profesional.getNombre().equalsIgnoreCase(String.valueOf(cita.getNutricionista()))))
                .toList();
    }

    // Día de la semana: lunes = 1 ... sábado = 6, igual que la ficha de cada nutricionista.
    private static boolean atiendeElDia(Nutricionista profesional, LocalDate fecha) {
        return profesional.getDias().contains(fecha.getDayOfWeek().getValue());
    }

    // Bloques de una hora que caben en la jornada (ej. 09:00 a 17:00 → 09:00 ... 16:00).
    private static List<LocalTime> bloquesDeJornada(Nutricionista profesional) {
        List<LocalTime> bloques = new ArrayList<>();
        LocalTime hasta = LocalTime.parse(profesional.getHasta());
        for (LocalTime hora = LocalTime.parse(profesional.getDesde()); !hora.plusHours(1).isAfter(hasta); hora = hora.plusHours(1)) {
            bloques.add(hora);
        }
        return bloques;
    }

    private static boolean atiendeEn(Nutricionista profesional, LocalDateTime momento) {
        return atiendeElDia(profesional, momento.toLocalDate()) && bloquesDeJornada(profesional).contains(momento.toLocalTime());
    }

    // ---------- Apoyo ----------

    private Cita obtener(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La cita no existe."));
    }

    // Acepta el nombre ("Carolina Fuentes") o el código ("NUT001") del nutricionista.
    private Nutricionista buscarNutricionista(String nombreOCodigo) {
        String buscado = nombreOCodigo.trim();
        return nutricionistaRepository.findAll().stream()
                .filter(candidato -> candidato.getNombre().equalsIgnoreCase(buscado) || candidato.getId().equalsIgnoreCase(buscado))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el nutricionista."));
    }

    private void exigirPacienteDuenoOPersonal(Cita cita, Jwt usuario) {
        String rol = rolDe(usuario);
        boolean personal = ADMINISTRADOR.equals(rol) || RECEPCION.equals(rol);
        boolean dueno = PACIENTE.equals(rol) && cita.getPacienteCorreo().equalsIgnoreCase(correoDe(usuario));
        if (!personal && !dueno) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta cita.");
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

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String rolDe(Jwt usuario) {
        String rol = usuario.getClaimAsString("rol");
        return rol == null ? "" : rol;
    }

    private static String correoDe(Jwt usuario) {
        return usuario.getClaimAsString("correo");
    }
}
