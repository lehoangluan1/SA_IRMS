package SA.irms.common.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import SA.irms.common.api.ApiErrorResponse;
import SA.irms.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(DomainException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception instanceof ValidationException validationException
                ? validationException.fieldErrors()
                : List.of();
        return ResponseEntity.status(exception.status())
                .body(errorResponse(exception.code(), exception.getMessage(), fieldErrors, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(errorResponse("validation_error", "Request validation failed.", fieldErrors, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        "validation_error",
                        "Missing required request parameter: " + exception.getParameterName() + ".",
                        List.of(new ApiErrorResponse.FieldError(exception.getParameterName(), "Parameter is required.")),
                        request
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String field = exception.getName() == null ? "request" : exception.getName();
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        "validation_error",
                        "Request parameter type is invalid.",
                        List.of(new ApiErrorResponse.FieldError(field, "Value is invalid.")),
                        request
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse("forbidden", "You do not have access to this operation.", List.of(), request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorResponse(
                        "method_not_allowed",
                        "Request method '" + exception.getMethod() + "' is not supported for this endpoint.",
                        List.of(),
                        request
                ));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        "not_found",
                        "No API endpoint exists for " + request.getMethod() + " " + request.getRequestURI() + ".",
                        List.of(),
                        request
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API error at {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("internal_error", "The request could not be completed.", List.of(), request));
    }

    private ApiErrorResponse errorResponse(
            String code,
            String message,
            List<ApiErrorResponse.FieldError> fieldErrors,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                code,
                message,
                RequestContext.getCorrelationId(request),
                Instant.now(),
                fieldErrors
        );
    }
}
