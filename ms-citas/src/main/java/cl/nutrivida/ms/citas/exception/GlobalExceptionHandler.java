// Convierte los errores en un JSON claro { status, mensaje } (y "campos" en errores de validación),
// con el mismo formato que los demás microservicios.

package cl.nutrivida.ms.citas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HorarioOcupadoException.class)
    public ResponseEntity<Map<String, Object>> manejarHorarioOcupado(HorarioOcupadoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(HorarioNoLaboralException.class)
    public ResponseEntity<Map<String, Object>> manejarHorarioNoLaboral(HorarioNoLaboralException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> negocio(ApiException ex) {
        return respuesta(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "mensaje", "Hay datos inválidos.", "campos", campos));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> sinPermiso(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.");
    }

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "mensaje", mensaje));
    }
}
