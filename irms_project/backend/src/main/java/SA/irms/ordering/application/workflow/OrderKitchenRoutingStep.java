package SA.irms.ordering.application.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.ordering.application.KitchenCoordinationPort;
import SA.irms.ordering.application.KitchenCoordinationPort.KitchenOrderItemCommand;

@Component
public class OrderKitchenRoutingStep implements OrderFulfillmentStep<OrderConfirmationContext> {
    private final KitchenCoordinationPort kitchenOrderRoutingPort;

    public OrderKitchenRoutingStep(KitchenCoordinationPort kitchenOrderRoutingPort) {
        this.kitchenOrderRoutingPort = kitchenOrderRoutingPort;
    }

    @Override
    public String name() {
        return "KITCHEN_ROUTED";
    }

    @Override
    public Map<String, Object> execute(OrderConfirmationContext context) {
        List<KitchenOrderItemCommand> items = extractKitchenItems(context.event().payload());
        kitchenOrderRoutingPort.queueConfirmedItems(context.orderId(), items, string(context.event().payload(), "specialInstructions", null));
        return Map.of(
                "orderId", context.orderId().toString(),
                "expectedOrderItemIds", items.stream().map(item -> item.orderItemId().toString()).toList(),
                "readyOrderItemIds", List.of(),
                "stationCount", items.stream().map(KitchenOrderItemCommand::station).distinct().count()
        );
    }

    @SuppressWarnings("unchecked")
    private List<KitchenOrderItemCommand> extractKitchenItems(Map<String, Object> payload) {
        Object value = payload.get("items");
        if (!(value instanceof List<?> rawItems)) {
            throw new ConflictException("OrderConfirmed payload must include kitchen routable items.");
        }
        List<KitchenOrderItemCommand> commands = new ArrayList<>();
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                throw new ConflictException("OrderConfirmed item payload is malformed.");
            }
            Map<String, Object> item = (Map<String, Object>) rawMap;
            commands.add(new KitchenOrderItemCommand(
                    UUID.fromString(item.get("orderItemId").toString()),
                    item.get("station").toString(),
                    Integer.parseInt(item.get("quantity").toString())
            ));
        }
        return commands;
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
