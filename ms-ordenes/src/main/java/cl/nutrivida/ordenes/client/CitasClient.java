// Comunicación entre microservicios: al confirmar el pago, ms-ordenes le pide a ms-citas que agende lo pagado
// (citas confirmadas, planes contratados y citas con pago en clínica que se pagan online).
// Usa el token de servicio de ms-ordenes (rol SERVICIO), no el del paciente.

package cl.nutrivida.ordenes.client;

import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.security.TokenServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitasClient {

    /** Una atención pagada: un plan puede ir sin fecha (sin primera sesión). */
    public record ItemPagado(String codigoServicio, LocalDateTime fechaHora, String nutricionista, String motivo) { }

    public record PagoRequest(String ordenId, String pacienteCorreo, String correoConfirmacion,
                              List<ItemPagado> items, List<Long> citaIds) { }

    public record LineaPagada(Long citaId, Long planId, String codigoServicio, String nombreServicio,
                              LocalDateTime fechaHora, String nutricionista, BigDecimal precio) { }

    public record PagoResponse(List<LineaPagada> lineas, BigDecimal total) { }

    private final RestClient http;
    private final TokenServicio tokenServicio;

    public CitasClient(@Value("${app.servicios.citas-url}") String citasUrl, TokenServicio tokenServicio) {
        this.tokenServicio = tokenServicio;
        this.http = RestClient.builder().baseUrl(citasUrl).build();
    }

    public PagoResponse registrarPago(PagoRequest pago) {
        try {
            return http.post()
                    .uri("/api/v1/citas/pagos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenServicio.emitir())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(pago)
                    .retrieve()
                    .body(PagoResponse.class);
        } catch (HttpClientErrorException e) {
            // ms-citas explica qué atención ya no se puede agendar (horario ocupado, menos de 2 h, etc.).
            throw new ApiException(HttpStatus.CONFLICT, "No se realizó ningún cobro: " + mensajeDe(e));
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La agenda no está disponible en este momento. No se realizó ningún cobro.");
        }
    }

    // Las respuestas de error de la API son {"status": ..., "mensaje": "..."}: se toma el mensaje.
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static String mensajeDe(HttpClientErrorException e) {
        Matcher encontrado = MENSAJE.matcher(e.getResponseBodyAsString());
        return encontrado.find() ? encontrado.group(1) : "una de las atenciones ya no se puede agendar.";
    }
}
