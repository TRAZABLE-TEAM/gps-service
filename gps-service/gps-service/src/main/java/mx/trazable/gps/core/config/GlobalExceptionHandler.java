package mx.trazable.gps.core.config;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import mx.trazable.gps.core.exception.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebServiceResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);
        //saveLog(ex, request, HttpStatus.BAD_REQUEST.value());

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Datos inválidos");

        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<WebServiceResponse> handlePermission(
            PermissionException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(
                HttpStatus.FORBIDDEN,
                "No tienes autorización para ejecutar esta acción"
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<WebServiceResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(
                HttpStatus.NOT_FOUND,
                getMessageOrDefault(ex, "Recurso no encontrado")
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebServiceResponse> handleBadJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        logException(ex, true);

        String message = "json-not-readable";

        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            message = ex.getMostSpecificCause().getMessage().split("\n")[0];
        }

        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<WebServiceResponse> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<WebServiceResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WebServiceResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WebServiceResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(
                HttpStatus.BAD_REQUEST,
                getMessageOrDefault(ex, "Solicitud inválida")
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<WebServiceResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(
                HttpStatus.CONFLICT,
                getMessageOrDefault(ex, "Conflicto en la solicitud")
        );
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<WebServiceResponse> handleExternalService(
            ExternalServiceException ex,
            HttpServletRequest request
    ) {
        logException(ex, true);

        return build(HttpStatus.BAD_GATEWAY, getMessageOrDefault(ex, "Error al consumir un servicio externo"));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebServiceResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        logException(ex, true);

        String traceId = MDC.get("traceId");

        String message = traceId != null && !traceId.isBlank()
                ? "Ocurrió un error interno. Código de seguimiento: " + traceId
                : "Ocurrió un error interno";

        return build(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private ResponseEntity<WebServiceResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new WebServiceResponse(false, message));
    }

    private void logException(Exception ex, boolean stacktrace) {
        String traceId = MDC.get("traceId");




            log.warn("Excepción atrapada. traceId={}, exception={}, message={}",
                    traceId,
                    ex.getClass().getSimpleName(),
                    getShortMessage(ex.getMessage())
            );
            return;

    }

    private String getShortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Sin mensaje";
        }

        message = message.replaceAll("\\s+", " ");

        return message.length() > 150
                ? message.substring(0, 150) + "..."
                : message;
    }

    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<WebServiceResponse> handleJwtExpired(
            JwtExpiredException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.UNAUTHORIZED, getMessageOrDefault(ex, "Tu sesión expiró. Inicia sesión nuevamente"));
    }

    @ExceptionHandler(BadValidationException.class)
    public ResponseEntity<WebServiceResponse> handleBadValidation(
            BadValidationException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.BAD_REQUEST, getMessageOrDefault(ex, "Solicitud inválida dentro de la validación"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<WebServiceResponse> handleCustomBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        logException(ex, false);

        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }


    private String getMessageOrDefault(Exception ex, String defaultMessage) {
        return ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : defaultMessage;
    }
}