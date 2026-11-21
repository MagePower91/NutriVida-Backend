// Planes contratados (versión simplificada de RN-13): el paciente ve sus planes con el saldo de sesiones
// y agenda la siguiente sin volver a pagar. Una sesión atendida se descuenta del saldo; una cancelada
// o con inasistencia vuelve a él. El plan finaliza cuando todas sus sesiones fueron atendidas.

package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.dto.PagoDtos.PlanResponse;
import cl.nutrivida.ms.citas.dto.PagoDtos.SesionRequest;
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

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final CitaRepository citaRepository;
    private final PagoService pagoService;

    public PlanService(PlanRepository planRepository, CitaRepository citaRepository, PagoService pagoService) {
        this.planRepository = planRepository;
        this.citaRepository = citaRepository;
        this.pagoService = pagoService;
    }

    /** Resumen de sesiones de un plan (mismo cálculo que el frontend, src/domain/planes.js). */
    public record Resumen(int atendidas, int agendadas, int porAgendar) { }

    public static Resumen resumir(Plan plan, List<Cita> sesiones) {
        int atendidas = (int) sesiones.stream().filter(sesion -> sesion.getEstadoCita() == EstadoCita.ATENDIDA).count();
        int agendadas = (int) sesiones.stream().filter(ReglasCita::estaActiva).count();
        return new Resumen(atendidas, agendadas, Math.max(0, plan.getSesionesTotales() - atendidas - agendadas));
    }

    // Paciente: sus planes. Administración y recepción: todos.
    @Transactional(readOnly = true)
    public List<PlanResponse> listar(Jwt usuario) {
        List<Plan> planes = esPersonal(usuario)
                ? planRepository.findAllByOrderByCreadoEnDesc()
                : planRepository.findByPacienteCorreoIgnoreCaseOrderByCreadoEnDesc(usuario.getClaimAsString("correo"));
        return planes.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(Long id, Jwt usuario) {
        return aRespuesta(planVisible(id, usuario));
    }

    // Agenda otra sesión del plan, sin volver a pagar, mientras queden sesiones por agendar.
    @Transactional
    public Cita agendarSesion(Long id, SesionRequest datos, Jwt usuario) {
        Plan plan = planVisible(id, usuario);
        Resumen resumen = resumir(plan, sesionesDe(plan));
        if (estadoVisible(plan, resumen) != EstadoPlan.ACTIVO) {
            throw new ApiException(HttpStatus.CONFLICT, "Este plan no está activo.");
        }
        if (resumen.porAgendar() == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya agendaste todas las sesiones de tu plan.");
        }
        return pagoService.crearSesion(plan, plan.getPacienteCorreo(), datos.fechaHora(), datos.nutricionista());
    }

    private Plan planVisible(Long id, Jwt usuario) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El plan no existe."));
        boolean dueno = plan.getPacienteCorreo().equalsIgnoreCase(String.valueOf(usuario.getClaimAsString("correo")));
        if (!dueno && !esPersonal(usuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para ver este plan.");
        }
        return plan;
    }

    private List<Cita> sesionesDe(Plan plan) {
        return citaRepository.findByPlanId(String.valueOf(plan.getId()));
    }

    // Un plan activo con todas sus sesiones atendidas se muestra como finalizado.
    private static EstadoPlan estadoVisible(Plan plan, Resumen resumen) {
        boolean completo = resumen.atendidas() >= plan.getSesionesTotales();
        return plan.getEstadoPlan() == EstadoPlan.ACTIVO && completo ? EstadoPlan.FINALIZADO : plan.getEstadoPlan();
    }

    private PlanResponse aRespuesta(Plan plan) {
        Resumen resumen = resumir(plan, sesionesDe(plan));
        return new PlanResponse(plan.getId(), plan.getPacienteCorreo(), plan.getCodigoServicio(), plan.getNombre(),
                plan.getSesionesTotales(), plan.getMontoPagado(), estadoVisible(plan, resumen).name(), plan.getOrdenId(),
                plan.getCreadoEn(), resumen.atendidas(), resumen.agendadas(), resumen.porAgendar());
    }

    private static boolean esPersonal(Jwt usuario) {
        String rol = usuario.getClaimAsString("rol");
        return "ADMINISTRADOR".equals(rol) || "RECEPCION".equals(rol);
    }
}
