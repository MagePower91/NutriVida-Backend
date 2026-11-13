package cl.nutrivida.identidad.controller;

import cl.nutrivida.identidad.dto.EstadoRequest;
import cl.nutrivida.identidad.dto.UsuarioRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.exception.ApiException;
import cl.nutrivida.identidad.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "CRUD de usuarios. Administrador gestiona; Recepción solo consulta.")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Listar usuarios", description = "Administrador y Recepción. Filtro opcional ?tipo=Nutricionista")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCION')")
    public List<UsuarioResponse> listar(@RequestParam(name = "tipo", required = false) String tipo) {
        return service.listar(tipo);
    }

    @Operation(summary = "Ver un usuario", description = "Administrador y Recepción ven a cualquiera; los demás solo a sí mismos.")
    @GetMapping("/{run}")
    public UsuarioResponse obtener(@PathVariable("run") String run, @AuthenticationPrincipal Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        boolean esPersonal = "ADMINISTRADOR".equals(rol) || "RECEPCION".equals(rol);
        if (!esPersonal && !run.equalsIgnoreCase(jwt.getSubject())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios datos.");
        }
        return service.obtener(run);
    }

    @Operation(summary = "Crear usuario", description = "Solo Administrador.")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return service.crear(req);
    }

    @Operation(summary = "Editar usuario", description = "Solo Administrador. Contraseña vacía = se mantiene.")
    @PutMapping("/{run}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse actualizar(@PathVariable("run") String run, @Valid @RequestBody UsuarioRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        return service.actualizar(run, req, jwt.getSubject());
    }

    @Operation(summary = "Activar o desactivar", description = "Solo Administrador. Desactivar cierra sus sesiones.")
    @PatchMapping("/{run}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse cambiarEstado(@PathVariable("run") String run, @Valid @RequestBody EstadoRequest req,
                                         @AuthenticationPrincipal Jwt jwt) {
        return service.cambiarEstado(run, req.activo(), jwt.getSubject());
    }

    @Operation(summary = "Eliminar usuario", description = "Solo Administrador.")
    @DeleteMapping("/{run}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("run") String run, @AuthenticationPrincipal Jwt jwt) {
        service.eliminar(run, jwt.getSubject());
    }
}