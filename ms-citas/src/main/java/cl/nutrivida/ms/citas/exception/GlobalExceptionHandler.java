
package cl.nutrivida.ms.citas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HorarioOcupadoException.class)
    public ResponseEntity<Map<String, String>> manejarHorarioOcupado(
            HorarioOcupadoException ex) {

        Map<String, String> respuesta = Map.of(
                "error", "Horario no disponible",
                "mensaje", ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }
    @ExceptionHandler(HorarioNoLaboralException.class)
    public ResponseEntity<Map<String, String>> manejarHorarioNoLaboral(
        HorarioNoLaboralException ex) {

        Map<String, String> respuesta = Map.of(
            "error", "Horario no válido",
            "mensaje", ex.getMessage()
         );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(respuesta);
    }
}
