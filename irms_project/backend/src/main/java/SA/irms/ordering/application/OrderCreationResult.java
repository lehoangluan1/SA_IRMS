package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import SA.irms.ordering.application.KitchenCoordinationPort.KitchenOrderItemCommand;

record OrderCreationResult(
        BigDecimal subtotal,
        List<KitchenOrderItemCommand> kitchenItems,
        List<Map<String, Object>> comboPayloads
) {
}
