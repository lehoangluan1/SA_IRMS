package SA.irms.kitchen.application.query;

import java.time.Instant;
import java.util.UUID;

public record KitchenTicketItemContext(
        UUID ticketItemId,
        UUID ticketId,
        UUID orderItemId,
        String status,
        UUID serverUserId,
        String dishName,
        Instant servedAt,
        Instant nextActionAt,
        Instant cookingStartedAt,
        Instant inventoryDeductedAt,
        String tableCode,
        String stationKind
) {
}
