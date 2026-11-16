package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.service.NutricionistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// El CORS lo maneja el api-gateway, por eso aquí no hay @CrossOrigin.
@RestController
@RequestMapping("/api/v1/nutricionistas")
@Tag(name = "Nutricionistas", description = "Profesionales, especialidades y jornada de atención")
public class NutricionistaController {

    private final NutricionistaService nutricionistaService;

    public NutricionistaController(NutricionistaService nutricionistaService) {
        this.nutricionistaService = nutricionistaService;
    }

    @Operation(summary = "Listar nutricionistas", description = "Requiere sesión.")
    @GetMapping
    public ResponseEntity<List<Nutricionista>> listarNutricionistas() {
        return ResponseEntity.ok(nutricionistaService.listarNutricionistas());
    }

    @Operation(summary = "Ver un nutricionista", description = "Requiere sesión.")
    @GetMapping("/{id}")
    public ResponseEntity<Nutricionista> buscarPorId(@PathVariable String id) {
        return nutricionistaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear o actualizar un nutricionista", description = "Solo administración.")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Nutricionista> guardarNutricionista(@RequestBody Nutricionista nutricionista) {
        return ResponseEntity.ok(nutricionistaService.guardarNutricionista(nutricionista));
    }

    @Operation(summary = "Eliminar un nutricionista", description = "Solo administración.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarNutricionista(@PathVariable String id) {
        nutricionistaService.eliminarNutricionista(id);
        return ResponseEntity.noContent().build();
    }
}
