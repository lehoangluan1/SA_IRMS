package SA.irms.common.api;

import java.util.UUID;

public record EntityReferenceResponse(
        String entityType,
        UUID entityId
) {
}
