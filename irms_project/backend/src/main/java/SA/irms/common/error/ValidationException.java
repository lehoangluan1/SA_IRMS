package SA.irms.common.error;

import java.util.List;

import org.springframework.http.HttpStatus;

import SA.irms.common.api.ApiErrorResponse;

public class ValidationException extends DomainException {
    private final List<ApiErrorResponse.FieldError> fieldErrors;

    public ValidationException(String message, List<ApiErrorResponse.FieldError> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "validation_error", message);
        this.fieldErrors = fieldErrors;
    }

    public List<ApiErrorResponse.FieldError> fieldErrors() {
        return fieldErrors;
    }
}
