
package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.dto.PagoDtos.PlanResponse;
import cl.nutrivida.ms.citas.dto.PagoDtos.SesionRequest;
import cl.nutrivida.ms.citas.dto.PlanesDtos.RegistrarPlanRequest;
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
@Tag(name = "Planes", description = "Registro, consulta y agenda de planes nutricionales")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(
        summary = "Registrar un plan comprado",
        description = "Operación interna invocada por ms-ordenes después de confirmar el pago."
    )
    @PostMapping
    @PreAuthorize("hasRole('SERVICIO')")
    public ResponseEntity<cl.nutrivida.ms.citas.dto.PlanesDtos.PlanResponse> registrarPlan(
            @Valid @RequestBody RegistrarPlanRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.registrarPlan(datos));
    }

    @Operation(
        summary = "Listar planes",
        description = "Paciente: sus planes. Administración y recepción: todos."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMINISTRADOR', 'RECEPCION')")
    public List<PlanResponse> listar(@AuthenticationPrincipal Jwt usuario) {
        return planService.listar(usuario);
    }

    @Operation(summary = "Consultar un plan")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMINISTRADOR', 'RECEPCION')")
    public PlanResponse obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt usuario) {

        return planService.obtener(id, usuario);
    }

    @Operation(
        summary = "Agendar una sesión del plan",
        description = "Agenda una sesión sin volver a pagar, mientras queden sesiones disponibles."
    )
    @PostMapping("/{id}/sesiones")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<Cita> agendarSesion(
            @PathVariable Long id,
            @Valid @RequestBody SesionRequest datos,
            @AuthenticationPrincipal Jwt usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.agendarSesion(id, datos, usuario));
    }
}