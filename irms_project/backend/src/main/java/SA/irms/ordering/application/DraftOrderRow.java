package SA.irms.ordering.application;

import java.util.UUID;

public record DraftOrderRow(UUID orderId, UUID tableSessionId, String status, String specialInstructions) {
}
