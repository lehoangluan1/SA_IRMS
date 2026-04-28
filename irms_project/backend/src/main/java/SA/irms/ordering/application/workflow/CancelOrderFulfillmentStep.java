package SA.irms.ordering.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CancelOrderFulfillmentStep {
    public Map<String, Object> execute(OrderCancellationContext context) {
        return Map.of(
                "orderId", context.orderId(),
                "reason", context.reason(),
                "compensationState", "COMPENSATION_REQUESTED"
        );
    }
}
