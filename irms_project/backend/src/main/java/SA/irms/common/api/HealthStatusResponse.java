package SA.irms.common.api;

import java.time.Instant;

public record HealthStatusResponse(
        String service,
        String role,
        String status,
        String database,
        Instant timestamp
) {
}
