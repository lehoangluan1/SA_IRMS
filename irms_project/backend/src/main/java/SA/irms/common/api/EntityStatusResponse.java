package SA.irms.common.api;

import java.util.UUID;

public record EntityStatusResponse(
        String entityType,
        UUID entityId,
        String status
) {
}
