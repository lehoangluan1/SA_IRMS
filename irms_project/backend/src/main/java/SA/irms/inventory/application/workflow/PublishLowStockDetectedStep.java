package SA.irms.inventory.application.workflow;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.inventory.application.events.LowStockDetectedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class PublishLowStockDetectedStep {
    private final DomainEventPublisher outboxPublisher;

    public PublishLowStockDetectedStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void execute(EventEnvelope event, EvaluateLowStockStep.Evaluation evaluation) {
        outboxPublisher.publish(new LowStockDetectedEvent(evaluation.inventoryItemId().toString(), evaluation.payload()),
                event.metadata().correlationId(), event.metadata().eventId().toString());
    }
}
