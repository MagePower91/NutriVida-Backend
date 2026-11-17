package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.dto.PagoDtos.PlanResponse;
import cl.nutrivida.ms.citas.dto.PagoDtos.SesionRequest;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes")
@PreAuthorize("hasAnyRole('PACIENTE', 'ADMINISTRADOR', 'RECEPCION')")
@Tag(name = "Planes", description = "Planes de varias sesiones: se pagan una vez (en el carrito) y se agendan de a una")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Listar planes", description = "Paciente: los suyos. Administración y recepción: todos. "
            + "Incluye sesiones atendidas, agendadas y por agendar.")
    @GetMapping
    public List<PlanResponse> listar(@AuthenticationPrincipal Jwt usuario) {
        return planService.listar(usuario);
    }

    @Operation(summary = "Ver un plan")
    @GetMapping("/{id}")
    public PlanResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt usuario) {
        return planService.obtener(id, usuario);
    }

    @Operation(summary = "Agendar una sesión del plan",
            description = "Sin volver a pagar, mientras queden sesiones. Mismas reglas de horario que una cita (2 h de anticipación).")
    @PostMapping("/{id}/sesiones")
    public ResponseEntity<Cita> agendarSesion(@PathVariable Long id, @Valid @RequestBody SesionRequest datos,
                                              @AuthenticationPrincipal Jwt usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.agendarSesion(id, datos, usuario));
    }
}
