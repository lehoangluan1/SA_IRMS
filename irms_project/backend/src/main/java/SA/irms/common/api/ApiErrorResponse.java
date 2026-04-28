package SA.irms.common.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(
            String field,
            String message
    ) {
    }
}
