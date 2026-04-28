package SA.irms.common.api;

import java.time.Instant;

public record ApiEnvelope<T>(
        T data,
        Meta meta
) {
    public static <T> ApiEnvelope<T> of(T data, String correlationId) {
        return new ApiEnvelope<>(data, new Meta(correlationId, Instant.now()));
    }

    public record Meta(
            String correlationId,
            Instant timestamp
    ) {
    }
}
