package cl.nutrivida.ordenes.controller;

import cl.nutrivida.ordenes.dto.OrdenesDtos.*;
import cl.nutrivida.ordenes.service.CarritoService;
import cl.nutrivida.ordenes.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carrito")
@PreAuthorize("hasRole('PACIENTE')")
@Tag(name = "Carrito", description = "Solo pacientes. Atenciones por pagar y checkout.")
public class CarritoController {

    private final CarritoService carrito;
    private final OrdenService ordenes;

    public CarritoController(CarritoService carrito, OrdenService ordenes) {
        this.carrito = carrito;
        this.ordenes = ordenes;
    }

    @Operation(summary = "Ver mi carrito", description = "Incluye cantidad y total.")
    @GetMapping
    public CarritoResponse ver(@AuthenticationPrincipal Jwt jwt) {
        return carrito.ver(jwt.getSubject());
    }

    @Operation(summary = "Agregar atención", description = "Consulta precio y disponibilidad en ms-catalogo.")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse agregar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AgregarItemRequest req) {
        return carrito.agregar(jwt.getSubject(), req);
    }

    @Operation(summary = "Quitar atención")
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        carrito.quitar(jwt.getSubject(), id);
    }

    @Operation(summary = "Vaciar carrito")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vaciar(@AuthenticationPrincipal Jwt jwt) {
        carrito.vaciar(jwt.getSubject());
    }

    @Operation(summary = "Pagar (simulado)", description = "Tarjeta terminada en 0000 = rechazada. Genera la orden.")
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenResponse pagar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest req) {
        return ordenes.pagar(jwt, req);
    }
}