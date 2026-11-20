
package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.client.CatalogoClient;
import cl.nutrivida.ms.citas.dto.PagoDtos.PlanResponse;
import cl.nutrivida.ms.citas.dto.PagoDtos.SesionRequest;
import cl.nutrivida.ms.citas.dto.PlanesDtos.RegistrarPlanRequest;
import cl.nutrivida.ms.citas.exception.ApiException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.model.EstadoPlan;
import cl.nutrivida.ms.citas.model.Plan;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final CitaRepository citaRepository;
    private final PagoService pagoService;
    private final CatalogoClient catalogoClient;
    private final Clock reloj;

    public PlanService(
            PlanRepository planRepository,
            CitaRepository citaRepository,
            PagoService pagoService,
            CatalogoClient catalogoClient,
            Clock reloj
    ) {
        this.planRepository = planRepository;
        this.citaRepository = citaRepository;
        this.pagoService = pagoService;
        this.catalogoClient = catalogoClient;
        this.reloj = reloj;
    }

    public record Resumen(int atendidas, int agendadas, int porAgendar) { }

    public static Resumen resumir(Plan plan, List<Cita> sesiones) {
        int atendidas = (int) sesiones.stream()
                .filter(sesion -> sesion.getEstadoCita() == EstadoCita.ATENDIDA)
                .count();

        int agendadas = (int) sesiones.stream()
                .filter(ReglasCita::estaActiva)
                .count();

        return new Resumen(
                atendidas,
                agendadas,
                Math.max(0, plan.getSesionesTotales() - atendidas - agendadas)
        );
    }

    // RN-13: registra un plan comprado en ms-ordenes.
    // La referencia única evita registrar dos veces la misma compra.
    @Transactional
    public cl.nutrivida.ms.citas.dto.PlanesDtos.PlanResponse registrarPlan(
            RegistrarPlanRequest datos) {

        var existente = planRepository.findByReferenciaCompra(
                datos.referenciaCompra()
        );

        if (existente.isPresent()) {
            return convertir(existente.get());
        }

        var servicio = catalogoClient.obtenerServicio(datos.codigoServicio());

        if (!servicio.activo()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El plan ya no se encuentra activo en el catálogo."
            );
        }

        if (!"Plan especializado".equalsIgnoreCase(servicio.tipo())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El servicio indicado no corresponde a un plan nutricional."
            );
        }

        if (!servicio.esPlan()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El plan no tiene una cantidad válida de sesiones."
            );
        }

        if (datos.sesionesTotales() != servicio.sesiones()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La cantidad de sesiones no coincide con el catálogo."
            );
        }

        Plan plan = new Plan();
        plan.setReferenciaCompra(datos.referenciaCompra());
        plan.setOrdenId(datos.ordenId());
        plan.setPacienteCorreo(datos.pacienteCorreo().trim().toLowerCase());
        plan.setCodigoServicio(servicio.codigo());
        plan.setNombre(servicio.nombre());
        plan.setSesionesTotales(servicio.sesiones());
        plan.setCreadoEn(LocalDateTime.now(reloj));

        return convertir(planRepository.save(plan));
    }

    // Paciente: sus planes. Administración y recepción: todos.
    @Transactional(readOnly = true)
    public List<PlanResponse> listar(Jwt usuario) {
        List<Plan> planes = esPersonal(usuario)
                ? planRepository.findAllByOrderByCreadoEnDesc()
                : planRepository.findByPacienteCorreoIgnoreCaseOrderByCreadoEnDesc(
                        correoDe(usuario)
                );

        return planes.stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(Long id, Jwt usuario) {
        return aRespuesta(planVisible(id, usuario));
    }

    // Agenda otra sesión sin volver a pagar.
    @Transactional
    public Cita agendarSesion(Long id, SesionRequest datos, Jwt usuario) {
        Plan plan = planVisible(id, usuario);
        Resumen resumen = resumir(plan, sesionesDe(plan));

        if (estadoVisible(plan, resumen) != EstadoPlan.ACTIVO) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Este plan no está activo."
            );
        }

        if (resumen.porAgendar() == 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya agendaste todas las sesiones de tu plan."
            );
        }

        return pagoService.crearSesion(
                plan,
                plan.getPacienteCorreo(),
                datos.fechaHora(),
                datos.nutricionista()
        );
    }

    private Plan planVisible(Long id, Jwt usuario) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "El plan no existe."
                ));

        boolean dueno = plan.getPacienteCorreo()
                .equalsIgnoreCase(correoDe(usuario));

        if (!dueno && !esPersonal(usuario)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para ver este plan."
            );
        }

        return plan;
    }

    private List<Cita> sesionesDe(Plan plan) {
        return citaRepository.findByPlanId(String.valueOf(plan.getId()));
    }

    private static EstadoPlan estadoVisible(Plan plan, Resumen resumen) {
        boolean completo = resumen.atendidas() >= plan.getSesionesTotales();

        return plan.getEstadoPlan() == EstadoPlan.ACTIVO && completo
                ? EstadoPlan.FINALIZADO
                : plan.getEstadoPlan();
    }

    // Respuesta utilizada por las consultas existentes de develop.
    private PlanResponse aRespuesta(Plan plan) {
        Resumen resumen = resumir(plan, sesionesDe(plan));

        return new PlanResponse(
                plan.getId(),
                plan.getPacienteCorreo(),
                plan.getCodigoServicio(),
                plan.getNombre(),
                plan.getSesionesTotales(),
                plan.getMontoPagado(),
                estadoVisible(plan, resumen).name(),
                plan.getOrdenId(),
                plan.getCreadoEn(),
                resumen.atendidas(),
                resumen.agendadas(),
                resumen.porAgendar()
        );
    }

    // Respuesta utilizada por el registro interno de RN-13.
    private cl.nutrivida.ms.citas.dto.PlanesDtos.PlanResponse convertir(Plan plan) {
        Resumen resumen = resumir(plan, sesionesDe(plan));

        return new cl.nutrivida.ms.citas.dto.PlanesDtos.PlanResponse(
                plan.getId(),
                plan.getReferenciaCompra(),
                plan.getOrdenId(),
                plan.getCodigoServicio(),
                plan.getNombreServicio(),
                plan.getSesionesTotales(),
                resumen.atendidas(),
                resumen.agendadas(),
                resumen.porAgendar(),
                resumen.atendidas() >= plan.getSesionesTotales(),
                plan.getCreadoEn()
        );
    }

    private String correoDe(Jwt usuario) {
        String correo = usuario.getClaimAsString("correo");

        if (correo == null || correo.isBlank()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "El token no contiene un correo válido."
            );
        }

        return correo.trim().toLowerCase();
    }

    private static boolean esPersonal(Jwt usuario) {
        String rol = usuario.getClaimAsString("rol");
        return "ADMINISTRADOR".equals(rol) || "RECEPCION".equals(rol);
    }
}
