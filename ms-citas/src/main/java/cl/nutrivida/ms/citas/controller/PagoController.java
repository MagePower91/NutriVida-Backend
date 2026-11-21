package cl.nutrivida.ms.citas.controller;

import cl.nutrivida.ms.citas.dto.PagoDtos.PagoRequest;
import cl.nutrivida.ms.citas.dto.PagoDtos.PagoResponse;
import cl.nutrivida.ms.citas.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Llamada interna entre microservicios: solo ms-ordenes, con su token de servicio (rol SERVICIO).
// El API Gateway bloquea esta ruta para que nunca se llame desde el navegador.
@RestController
@RequestMapping("/api/v1/citas/pagos")
@Tag(name = "Pagos (interno)", description = "Uso exclusivo de ms-ordenes al confirmar el pago del carrito")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Registrar un pago online",
            description = "Crea las citas pagadas, contrata los planes y marca como pagadas las citas con pago en clínica "
                    + "elegidas. Todo o nada. Requiere el token de servicio de ms-ordenes.")
    @PostMapping
    @PreAuthorize("hasRole('SERVICIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse registrar(@Valid @RequestBody PagoRequest datos) {
        return pagoService.registrarPagoOnline(datos);
    }
}
