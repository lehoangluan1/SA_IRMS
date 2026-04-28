package SA.irms.reservation.application.workflow;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class RequestReservationNotificationStep {
    private final DomainEventPublisher outboxPublisher;

    public RequestReservationNotificationStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void notifySeated(UUID reservationId, EventEnvelope event) {
        outboxPublisher.publish("NotificationRequested", 1, "Notification", reservationId.toString(), Map.of(
                "channel", "sms",
                "type", "reservation_seated",
                "title", "Table ready",
                "body", "Your table is ready.",
                "priority", "high"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }

    public void notifyWaitlist(UUID waitlistEntryId, EventEnvelope event) {
        outboxPublisher.publish("NotificationRequested", 1, "Notification", waitlistEntryId.toString(), Map.of(
                "channel", "sms",
                "type", "waitlist_updated",
                "title", "Waitlist updated",
                "body", "Your waitlist status changed.",
                "priority", "medium"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
