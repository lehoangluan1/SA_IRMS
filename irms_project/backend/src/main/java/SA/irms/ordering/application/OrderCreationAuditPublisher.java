package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest;
import SA.irms.common.context.RequestMetadata;
import SA.irms.ordering.application.events.OrderConfirmedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
class OrderCreationAuditPublisher {
    private final AuditRecorder auditRecorder;
    private final DomainEventPublisher outboxPublisher;

    OrderCreationAuditPublisher(AuditRecorder auditRecorder, DomainEventPublisher outboxPublisher) {
        this.auditRecorder = auditRecorder;
        this.outboxPublisher = outboxPublisher;
    }

    void publishOrderConfirmed(UUID orderId, CreateOrderRequest request, OrderCreationResult created, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("tableSessionId", request.sessionId().toString());
        payload.put("specialInstructions", request.specialInstructions() == null ? "" : request.specialInstructions());
        payload.put("items", created.kitchenItems().stream().map(item -> Map.<String, Object>of(
                "orderItemId", item.orderItemId().toString(),
                "station", item.station(),
                "quantity", item.quantity()
        )).toList());
        payload.put("combos", created.comboPayloads());
        payload.put("subtotal", created.subtotal());
        outboxPublisher.publish(new OrderConfirmedEvent(orderId.toString(), payload), correlationId, null);
    }

    void auditDraftOrder(AuthenticatedUser actor, UUID orderId, String correlationId, RequestMetadata request,
                         UUID sessionId, int itemCount, BigDecimal subtotal) {
        auditOrder(actor, orderId, correlationId, request, "order.drafted", sessionId, itemCount, subtotal);
    }

    void auditConfirmedOrder(AuthenticatedUser actor, UUID orderId, String correlationId, RequestMetadata request,
                             UUID sessionId, int itemCount, BigDecimal subtotal) {
        auditOrder(actor, orderId, correlationId, request, "order.confirmed", sessionId, itemCount, subtotal);
    }

    private void auditOrder(AuthenticatedUser actor, UUID orderId, String correlationId, RequestMetadata request,
                            String action, UUID sessionId, int itemCount, BigDecimal subtotal) {
        auditRecorder.record(
                actor.userId(),
                action,
                "Order",
                orderId.toString(),
                correlationId,
                null,
                false,
                request.remoteIp(),
                Map.of(),
                Map.of("tableSessionId", sessionId, "itemCount", itemCount, "subtotal", subtotal)
        );
    }
}
