package SA.irms.kitchen.application;

import java.util.UUID;

public record KitchenOrderItemCommand(UUID orderItemId, String station, int quantity) {
}
