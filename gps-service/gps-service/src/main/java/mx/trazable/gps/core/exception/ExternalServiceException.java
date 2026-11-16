package mx.trazable.gps.core.exception;

/**
 * Error al consumir un servicio externo (ej. SAP). Se responde con 502 Bad Gateway
 * y un mensaje claro, en lugar del 500 generico.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
