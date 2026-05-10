package SA.irms.ordering.application.workflow;

import java.util.UUID;

import SA.irms.common.events.EventEnvelope;

public record OrderConfirmationContext(UUID orderId, EventEnvelope event) {
}
