//la conexión con ms-cat

package cl.nutrivida.ordenes.client;

import cl.nutrivida.ordenes.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import java.util.Map;

/** Comunicación entre microservicios: ms-ordenes consulta y actualiza ms-catalogo por HTTP. */
@Component
public class CatalogoClient {

    /** Copia de lo que responde ms-catalogo en GET /api/v1/servicios/{codigo}. */
    public record ServicioCatalogo(
            String codigo, String nombre, Long categoriaId, String tipo,
            String duracion, String modalidad, String profesional,
            int precio, int descuento, int precioFinal,
            Integer sesiones, Integer cupos, String descripcion, boolean activo) { }

    private final RestClient http;

    public CatalogoClient(@Value("${app.servicios.catalogo-url}") String catalogoUrl) {
        this.http = RestClient.builder()
                .baseUrl(catalogoUrl)
                .requestFactory(new JdkClientHttpRequestFactory())   // soporta PATCH
                .build();
    }

    public ServicioCatalogo obtenerServicio(String codigo) {
        try {
            return http.get()
                    .uri("/api/v1/servicios/{codigo}", codigo)
                    .retrieve()
                    .body(ServicioCatalogo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El servicio " + codigo + " no existe en el catálogo.");
        } catch (ResourceAccessException e) {
            throw catalogoNoDisponible();
        }
    }

    /** Descuenta cupos de un taller. Reenvía el token del paciente porque el endpoint exige sesión. */
    public void reservarCupos(String codigo, int cantidad, String tokenUsuario) {
        try {
            http.patch()
                    .uri("/api/v1/servicios/{codigo}/cupos", codigo)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUsuario)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cantidad", cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se pudo reservar cupo en " + codigo + ": no quedan cupos suficientes.");
        } catch (ResourceAccessException e) {
            throw catalogoNoDisponible();
        }
    }

    private ApiException catalogoNoDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "El catálogo no está disponible en este momento. Intenta más tarde.");
    }
}