package SA.irms.kitchen.application.workflow;

import java.time.Instant;
import java.util.UUID;

import SA.irms.kitchen.domain.KitchenTicketState;
import SA.irms.common.events.EventEnvelope;

public record KitchenTicketTransitionContext(
        UUID ticketId,
        KitchenTicketState currentState,
        KitchenTicketState targetState,
        int preparationMinutes,
        int itemCount,
        int priority,
        boolean peakHour,
        Instant deadline,
        EventEnvelope workflowEvent
) {
}
