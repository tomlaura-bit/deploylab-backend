package edu.deploylab;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> uploadTooLarge(Exception e,HttpServletRequest req) { return error(413,"El archivo supera el límite de carga",req); }
    @ExceptionHandler(DeployLabException.class)
    ResponseEntity<ErrorResponse> domain(DeployLabException e,HttpServletRequest req) { return error(e.status(),e.getMessage(),req); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class})
    ResponseEntity<ErrorResponse> invalid(Exception e,HttpServletRequest req) { return error(400,"Solicitud inválida. Revisa los campos y sus formatos.",req); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(Exception e,HttpServletRequest req) { return error(409,"El registro ya existe o viola una relación de datos",req); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> notFound(Exception e,HttpServletRequest req) { return error(404,"Recurso no encontrado",req); }
    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<ErrorResponse> denied(Exception e,HttpServletRequest req) { return error(403,"Acceso denegado",req); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> state(IllegalStateException e,HttpServletRequest req) { return error(409,e.getMessage(),req); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e,HttpServletRequest req) { return error(500,"Ocurrió un error interno",req); }
    private ResponseEntity<ErrorResponse> error(int status,String message,HttpServletRequest req) {
        String reason=Objects.requireNonNullElse(HttpStatus.resolve(status),HttpStatus.INTERNAL_SERVER_ERROR).getReasonPhrase();
        return ResponseEntity.status(status).body(ErrorResponse.of(status,reason,message,req.getRequestURI()));
    }
}
