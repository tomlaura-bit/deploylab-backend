package edu.deploylab;

public class ApiException extends RuntimeException {
    public final int status;
    public ApiException(int status, String message) { super(message); this.status = status; }
    public static ApiException missing() { return new ApiException(404, "Recurso no encontrado"); }
    public static ApiException forbidden() { return new ApiException(403, "No tienes permiso para esta operación"); }
}
