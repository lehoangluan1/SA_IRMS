package SA.irms.notification.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.notification.application.port.out.NotificationQueuePort;

import SA.irms.notification.application.port.in.NotificationRequestConsumerUseCase;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Service
public class NotificationRequestMaterializer implements NotificationRequestConsumerUseCase {
    private final NotificationQueuePort notificationService;
    private final List<NotificationEventMapper> typedMappers;

    public NotificationRequestMaterializer(NotificationQueuePort notificationService, List<NotificationEventMapper> typedMappers) {
        this.notificationService = notificationService;
        this.typedMappers = List.copyOf(typedMappers);
    }

    @Override
    @Transactional
    public void materialize(EventEnvelope envelope) {
        NotificationCommand command = typedMappers.stream()
                .filter(mapper -> mapper.supports(envelope))
                .map(mapper -> mapper.map(envelope))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> genericCommand(envelope));
        notificationService.queue(command);
    }

    private NotificationCommand genericCommand(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return new NotificationCommand(
                uuid(payload, "lowStockAlertId"),
                uuid(payload, "reservationId"),
                uuid(payload, "waitlistEntryId"),
                uuid(payload, "paymentId"),
                string(payload, "channel", "in_app"),
                string(payload, "type", envelope.metadata().eventType()),
                string(payload, "templateCode", envelope.metadata().eventType()),
                payload,
                string(payload, "title", titleFor(envelope)),
                string(payload, "body", bodyFor(envelope)),
                string(payload, "recipientRole", null),
                uuid(payload, "recipientUserId"),
                string(payload, "recipientAddress", "manager"),
                string(payload, "priority", "medium")
        );
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private String titleFor(EventEnvelope envelope) {
        return switch (envelope.metadata().eventType()) {
            case ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED -> "Kitchen update";
            case ServiceEventTypes.INVENTORY_STOCK_CHANGED, ServiceEventTypes.LOW_STOCK_DETECTED -> "Inventory update";
            case ServiceEventTypes.PAYMENT_COMPLETED -> "Payment completed";
            case ServiceEventTypes.REFUND_ISSUED -> "Refund issued";
            case ServiceEventTypes.RECEIPT_GENERATED -> "Receipt generated";
            case ServiceEventTypes.ORDER_CANCELLED -> "Order cancelled";
            case ServiceEventTypes.RESERVATION_SEATED -> "Table ready";
            default -> envelope.metadata().eventType();
        };
    }

    private String bodyFor(EventEnvelope envelope) {
        return "Restaurant event " + envelope.metadata().eventType() + " for "
                + envelope.metadata().aggregateType() + " " + envelope.metadata().aggregateId();
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
