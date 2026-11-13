// Agrupa todos los datos que entran y salen del catálogo.
// El campo tipo de la respuesta lleva el nombre de la categoría, igual que en el frontend.

package cl.nutrivida.catalogo.dto;

import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.model.Servicio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public final class CatalogoDtos {

    private CatalogoDtos() { }

    // ---------- Categorías ----------

    public record CategoriaRequest(
            @Schema(example = "Consulta")
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
            String nombre,
            @Size(max = 255) String descripcion) { }

    public record CategoriaResponse(Long id, String nombre, String descripcion) {
        public static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
        }
    }

    // ---------- Servicios ----------

    public record ServicioRequest(
            @Schema(example = "CN006")
            @NotBlank(message = "El código es obligatorio.")
            @Pattern(regexp = "^[A-Za-z]{2}[0-9]{3}$", message = "El código debe tener 2 letras y 3 números (ej: CN006).")
            String codigo,
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
            String nombre,
            @Schema(example = "1")
            @NotNull(message = "La categoría es obligatoria.")
            Long categoriaId,
            @Schema(example = "30 min") @Size(max = 20) String duracion,
            @Schema(example = "Presencial") @Size(max = 40) String modalidad,
            @Schema(example = "Nutricionista") @Size(max = 40) String profesional,
            @Schema(example = "25000")
            @NotNull(message = "El precio es obligatorio.")
            @Positive(message = "El precio debe ser mayor a 0.")
            Integer precio,
            @Schema(example = "0")
            @NotNull(message = "El descuento es obligatorio (0 si no tiene).")
            @Min(value = 0, message = "El descuento no puede ser negativo.")
            @Max(value = 100, message = "El descuento no puede superar el 100%.")
            Integer descuento,
            @Schema(description = "Solo planes") @Min(value = 1, message = "Las sesiones deben ser al menos 1.") Integer sesiones,
            @Schema(description = "Solo talleres") @Min(value = 0, message = "Los cupos no pueden ser negativos.") Integer cupos,
            @Size(max = 500) String descripcion,
            @Schema(description = "Vacío = activo") Boolean activo) { }

    public record ServicioResponse(
            String codigo, String nombre, Long categoriaId, String tipo,
            String duracion, String modalidad, String profesional,
            int precio, int descuento, int precioFinal,
            Integer sesiones, Integer cupos, String descripcion, boolean activo) {

        public static ServicioResponse de(Servicio s) {
            return new ServicioResponse(s.getCodigo(), s.getNombre(),
                    s.getCategoria().getId(), s.getCategoria().getNombre(),
                    s.getDuracion(), s.getModalidad(), s.getProfesional(),
                    s.getPrecio(), s.getDescuento(), s.getPrecioFinal(),
                    s.getSesiones(), s.getCupos(), s.getDescripcion(), s.isActivo());
        }
    }

    public record CuposRequest(
            @Schema(example = "1")
            @NotNull(message = "La cantidad es obligatoria.")
            @Min(value = 1, message = "La cantidad debe ser al menos 1.")
            Integer cantidad) { }
}