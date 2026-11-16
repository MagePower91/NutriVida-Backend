package cl.nutrivida.catalogo.controller;

import cl.nutrivida.catalogo.dto.CatalogoDtos.CuposRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioResponse;
import cl.nutrivida.catalogo.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios")
@Tag(name = "Servicios", description = "Catálogo de consultas, planes, evaluaciones y talleres")
public class ServicioController {

    private final ServicioService service;

    public ServicioController(ServicioService service) {
        this.service = service;
    }

    @Operation(summary = "Listar servicios", description = "Público. Filtros opcionales por categoría y solo activos.")
    @GetMapping
    public List<ServicioResponse> listar(
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "soloActivos", defaultValue = "true") boolean soloActivos) {
        return service.listar(categoriaId, soloActivos);
    }

    @Operation(summary = "Ver un servicio", description = "Público. Incluye precio final con descuento.")
    @GetMapping("/{codigo}")
    public ServicioResponse obtener(@PathVariable("codigo") String codigo) {
        return service.obtener(codigo);
    }

    @Operation(summary = "Crear servicio", description = "Solo Administrador.")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioResponse crear(@Valid @RequestBody ServicioRequest req) {
        return service.crear(req);
    }

    @Operation(summary = "Editar servicio", description = "Solo Administrador. activo=false lo oculta del catálogo.")
    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ServicioResponse actualizar(@PathVariable("codigo") String codigo, @Valid @RequestBody ServicioRequest req) {
        return service.actualizar(codigo, req);
    }

    @Operation(summary = "Eliminar servicio", description = "Solo Administrador.")
    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("codigo") String codigo) {
        service.eliminar(codigo);
    }

    @Operation(summary = "Reservar cupos de un taller", description = "Requiere sesión. Lo usa ms-ordenes al pagar.")
    @PatchMapping("/{codigo}/cupos")
    public ServicioResponse reservarCupos(@PathVariable("codigo") String codigo, @Valid @RequestBody CuposRequest req) {
        return service.reservarCupos(codigo, req.cantidad());
    }
}