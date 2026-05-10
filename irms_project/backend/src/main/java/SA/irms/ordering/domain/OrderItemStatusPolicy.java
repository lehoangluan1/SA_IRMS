package SA.irms.ordering.domain;

import java.util.List;

import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;

public class OrderItemStatusPolicy {
    public void ensureDelayedItemCanBeSent(String orderStatus, String itemStatus) {
        if ("draft".equals(orderStatus)) {
            throw new ConflictException("Draft orders must be confirmed before delayed items can be sent to the kitchen.");
        }
        if (!"hold_for_service".equals(itemStatus)) {
            throw new ConflictException("Only delayed items can be sent to the kitchen.");
        }
    }

    public void ensureItemCanBeCancelled(String itemStatus, AuthenticatedUser actor) {
        if (List.of("pending", "sent_to_kitchen").contains(itemStatus)) {
            return;
        }
        if (actor.hasRole("manager") || actor.hasRole("admin")) {
            return;
        }
        throw new ConflictException("Manager approval is required to cancel an item after kitchen processing started.");
    }
}
