package cl.nutrivida.catalogo.controller;

import cl.nutrivida.catalogo.dto.CatalogoDtos.CategoriaRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.CategoriaResponse;
import cl.nutrivida.catalogo.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Consulta pública; gestión solo Administrador")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @Operation(summary = "Listar categorías", description = "Público.")
    @GetMapping
    public List<CategoriaResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Ver una categoría", description = "Público.")
    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear categoría", description = "Solo Administrador.")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest req) {
        return service.crear(req);
    }

    @Operation(summary = "Editar categoría", description = "Solo Administrador.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CategoriaResponse actualizar(@PathVariable("id") Long id, @Valid @RequestBody CategoriaRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Eliminar categoría", description = "Solo Administrador. Falla si tiene servicios.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
    }
}