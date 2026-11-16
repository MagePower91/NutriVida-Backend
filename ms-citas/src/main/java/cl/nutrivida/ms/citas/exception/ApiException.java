// Error de negocio con su código HTTP; GlobalExceptionHandler lo convierte en JSON.

package cl.nutrivida.ms.citas.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
