package warehouse_inventory.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> errorBody(
            int status, String message, String path) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("message", message);
        body.put("path", path);

        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        int status = ex.getStatusCode().value();

        String message = ex.getReason() == null
                ? "Request failed"
                : ex.getReason();

        return ResponseEntity.status(status).body(
                errorBody(status, message, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .orElse("Invalid input");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                errorBody(400, message, request.getRequestURI()));
    }
}