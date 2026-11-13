package cl.nutrivida.ordenes.dto;

import cl.nutrivida.ordenes.model.ItemCarrito;
import cl.nutrivida.ordenes.model.Orden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class OrdenesDtos {

    private OrdenesDtos() { }

    // ---------- Carrito ----------

    public record AgregarItemRequest(
            @Schema(example = "CN001")
            @NotBlank(message = "El código del servicio es obligatorio.")
            @Pattern(regexp = "^[A-Za-z]{2}[0-9]{3}$", message = "Código de servicio inválido (ej: CN001).")
            String codigoServicio,
            @Schema(example = "2026-12-15T10:00:00")
            @NotNull(message = "La fecha y hora es obligatoria.")
            @Future(message = "La fecha y hora debe ser futura.")
            LocalDateTime fechaHora,
            @Schema(example = "Carolina Fuentes M.")
            @Size(max = 100) String nutricionista) { }

    public record ItemResponse(Long id, String codigoServicio, String nombreServicio, int precio,
                               LocalDateTime fechaHora, String nutricionista) {
        public static ItemResponse de(ItemCarrito i) {
            return new ItemResponse(i.getId(), i.getCodigoServicio(), i.getNombreServicio(), i.getPrecio(),
                    i.getFechaHora(), i.getNutricionista());
        }
    }

    public record CarritoResponse(List<ItemResponse> items, int cantidad, int total, int maximo) { }

    // ---------- Pago ----------

    public record CheckoutRequest(
            @Schema(example = "Javiera Soto")
            @NotBlank(message = "El nombre del titular es obligatorio.")
            String titular,
            @Schema(example = "4111111111111111", description = "16 dígitos sin espacios. Terminada en 0000 = rechazada.")
            @NotBlank(message = "El número de tarjeta es obligatorio.")
            @Pattern(regexp = "^[0-9]{16}$", message = "El número de tarjeta debe tener 16 dígitos, sin espacios.")
            String numero,
            @Schema(example = "12/30")
            @NotBlank(message = "El vencimiento es obligatorio.")
            @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Usa el formato MM/AA.")
            String vencimiento,
            @Schema(example = "123")
            @NotBlank(message = "El CVV es obligatorio.")
            @Pattern(regexp = "^[0-9]{3}$", message = "El CVV debe tener 3 dígitos.")
            String cvv,
            @Schema(description = "IDs de las atenciones a pagar. Vacío = todo el carrito.")
            List<Long> itemIds) { }

    // ---------- Órdenes ----------

    public record DetalleResponse(String codigoServicio, String nombreServicio, int precio,
                                  LocalDateTime fechaHora, String nutricionista) { }

    public record OrdenResponse(Long id, String runPaciente, String correoPaciente, LocalDateTime fecha,
                                String estado, int total, String tarjeta, List<DetalleResponse> detalles) {
        public static OrdenResponse de(Orden o) {
            List<DetalleResponse> detalles = o.getDetalles().stream()
                    .map(d -> new DetalleResponse(d.getCodigoServicio(), d.getNombreServicio(), d.getPrecio(),
                            d.getFechaHora(), d.getNutricionista()))
                    .toList();
            return new OrdenResponse(o.getId(), o.getRunPaciente(), o.getCorreoPaciente(), o.getFecha(),
                    o.getEstado(), o.getTotal(), "**** " + o.getUltimos4Tarjeta(), detalles);
        }
    }
}