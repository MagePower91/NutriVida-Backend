// Comunicación entre microservicios: ms-citas consulta en ms-catalogo el nombre y el precio
// de un servicio. Así el precio de una cita lo define el catálogo, nunca el navegador.

package cl.nutrivida.ms.citas.client;

import cl.nutrivida.ms.citas.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogoClient {

    /** Lo que se usa de la respuesta de ms-catalogo en GET /api/v1/servicios/{codigo}. */
    public record ServicioCatalogo(String codigo, String nombre, String tipo, int precioFinal, boolean activo) { }

    private final RestClient http;

    public CatalogoClient(@Value("${app.servicios.catalogo-url}") String catalogoUrl) {
        this.http = RestClient.builder().baseUrl(catalogoUrl).build();
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
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo no está disponible en este momento. Intenta más tarde.");
        }
    }
}
