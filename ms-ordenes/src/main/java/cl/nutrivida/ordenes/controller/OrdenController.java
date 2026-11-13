package cl.nutrivida.ordenes.controller;

import cl.nutrivida.ordenes.dto.OrdenesDtos.OrdenResponse;
import cl.nutrivida.ordenes.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ordenes")
@Tag(name = "Órdenes", description = "Paciente ve las suyas; Administrador y Recepción ven todas.")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @Operation(summary = "Listar órdenes", description = "Según el rol de la sesión.")
    @GetMapping
    public List<OrdenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(jwt);
    }

    @Operation(summary = "Ver una orden", description = "El dueño, Administrador o Recepción.")
    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.obtener(id, jwt);
    }
}