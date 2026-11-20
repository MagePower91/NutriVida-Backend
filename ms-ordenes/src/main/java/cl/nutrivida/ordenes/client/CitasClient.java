
package cl.nutrivida.ordenes.client;

import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.security.TokenServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comunicación entre ms-ordenes y ms-citas.
 * Permite registrar pagos completos y registrar planes ya pagados.
 */
@Component
public class CitasClient {

    /** Una atención pagada. La referencia identifica de forma única cada plan. */
    public record ItemPagado(
            String codigoServicio,
            LocalDateTime fechaHora,
            String nutricionista,
            String motivo,
            String referenciaCompra
    ) {
        // Compatibilidad con llamadas que todavía usan cuatro parámetros.
        public ItemPagado(
                String codigoServicio,
                LocalDateTime fechaHora,
                String nutricionista,
                String motivo
        ) {
            this(codigoServicio, fechaHora, nutricionista, motivo, null);
        }
    }

    public record PagoRequest(
            String ordenId,
            String pacienteCorreo,
            String correoConfirmacion,
            List<ItemPagado> items,
            List<Long> citaIds
    ) { }

    public record LineaPagada(
            Long citaId,
            Long planId,
            String codigoServicio,
            String nombreServicio,
            LocalDateTime fechaHora,
            String nutricionista,
            BigDecimal precio
    ) { }

    public record PagoResponse(List<LineaPagada> lineas, BigDecimal total) { }

    /** Datos para registrar un plan que ya fue pagado. */
    public record RegistrarPlanRequest(
            String referenciaCompra,
            String ordenId,
            String pacienteCorreo,
            String codigoServicio,
            String nombreServicio,
            Integer sesionesTotales
    ) { }

    private final RestClient http;
    private final TokenServicio tokenServicio;

    public CitasClient(
            @Value("${app.servicios.citas-url}") String citasUrl,
            TokenServicio tokenServicio
    ) {
        this.tokenServicio = tokenServicio;
        this.http = RestClient.builder()
                .baseUrl(citasUrl)
                .build();
    }

    /** Envía el pago completo a ms-citas. */
    public PagoResponse registrarPago(PagoRequest pago) {
        try {
            return http.post()
                    .uri("/api/v1/citas/pagos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServicio.emitir())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(pago)
                    .retrieve()
                    .body(PagoResponse.class);

        } catch (RestClientResponseException e) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No se realizó ningún cobro: " + mensajeDe(e)
            );

        } catch (ResourceAccessException e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La agenda no está disponible en este momento. No se realizó ningún cobro."
            );
        }
    }

    /** Registra en ms-citas un plan que ya fue pagado. */
    public void registrarPlan(RegistrarPlanRequest datos) {
        try {
            http.post()
                    .uri("/api/v1/planes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServicio.emitir())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(datos)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException e) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No se pudo registrar el plan en el servicio de citas."
            );

        } catch (ResourceAccessException e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de citas no está disponible en este momento."
            );
        }
    }

    private static final Pattern MENSAJE =
            Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static String mensajeDe(RestClientResponseException e) {
        Matcher encontrado = MENSAJE.matcher(e.getResponseBodyAsString());
        return encontrado.find()
                ? encontrado.group(1)
                : "una de las atenciones ya no se puede agendar.";
    }
}
