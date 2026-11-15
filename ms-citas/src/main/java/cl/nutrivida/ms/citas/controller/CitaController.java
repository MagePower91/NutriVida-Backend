package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.dto.CitaRequest;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.service.CitaService;
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

// El CORS lo maneja el api-gateway, por eso aquí no hay @CrossOrigin.
@RestController
@RequestMapping("/api/v1/citas")
@Tag(name = "Citas", description = "Agenda de citas: cada rol ve solo lo que le corresponde")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @Operation(summary = "Listar citas",
            description = "Paciente: sus citas. Nutricionista: su agenda. Administración y recepción: todas.")
    @GetMapping
    public ResponseEntity<List<Cita>> listarCitas(@AuthenticationPrincipal Jwt usuario) {
        return ResponseEntity.ok(citaService.listarCitas(usuario));
    }

    @Operation(summary = "Ver una cita", description = "Solo su paciente, su nutricionista o el personal.")
    @GetMapping("/{id}")
    public ResponseEntity<Cita> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt usuario) {
        return ResponseEntity.ok(citaService.buscarPorId(id, usuario));
    }

    @Operation(summary = "Agendar una cita",
            description = "Paciente: para sí mismo. Administración y recepción: indicando pacienteCorreo. "
                    + "El precio lo define ms-catalogo y la cita queda por confirmar con pago pendiente.")
    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMINISTRADOR', 'RECEPCION')")
    public ResponseEntity<Cita> crearCita(@Valid @RequestBody CitaRequest datos, @AuthenticationPrincipal Jwt usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crearCita(datos, usuario));
    }

    @Operation(summary = "Eliminar una cita", description = "Solo administración.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarCita(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.noContent().build();
    }
}
