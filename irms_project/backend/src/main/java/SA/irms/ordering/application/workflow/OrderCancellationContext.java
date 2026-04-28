package SA.irms.ordering.application.workflow;

import SA.irms.common.events.EventEnvelope;

public record OrderCancellationContext(String orderId, EventEnvelope event, String reason) {
}
