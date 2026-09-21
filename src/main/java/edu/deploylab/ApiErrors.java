package edu.deploylab;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadTooLarge(Exception e) { return error(413,"El archivo supera el límite de carga"); }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> domain(ApiException e) { return error(e.status, e.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class})
    ResponseEntity<?> invalid(Exception e) { return error(400, "Solicitud inválida. Revisa los campos y sus formatos."); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e) { return error(409, "El registro ya existe o viola una relación de datos"); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> state(IllegalStateException e) { return error(409, e.getMessage()); }
    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    }
}
