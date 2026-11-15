package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.service.NutricionistaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutricionistas")
@CrossOrigin(origins = "*")
public class NutricionistaController {

    private final NutricionistaService nutricionistaService;

    public NutricionistaController(NutricionistaService nutricionistaService) {
        this.nutricionistaService = nutricionistaService;
    }

    @GetMapping
    public ResponseEntity<List<Nutricionista>> listarNutricionistas() {
        return ResponseEntity.ok(
                nutricionistaService.listarNutricionistas()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Nutricionista> buscarPorId(
            @PathVariable String id) {

        return nutricionistaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Nutricionista> guardarNutricionista(
            @RequestBody Nutricionista nutricionista) {

        return ResponseEntity.ok(
                nutricionistaService.guardarNutricionista(nutricionista)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNutricionista(
            @PathVariable String id) {

        nutricionistaService.eliminarNutricionista(id);
        return ResponseEntity.noContent().build();
    }
}