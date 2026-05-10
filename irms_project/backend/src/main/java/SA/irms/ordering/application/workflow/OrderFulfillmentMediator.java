package SA.irms.ordering.application.workflow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.ConflictException;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.workflow.WorkflowExecutionSupport;
import SA.irms.common.workflow.WorkflowInstance;
import SA.irms.common.workflow.WorkflowStepStatus;

@Service
public class OrderFulfillmentMediator {
    private static final String WORKFLOW_TYPE = "OrderFulfillment";
    private static final String AGGREGATE_TYPE = "Order";
    private static final Set<String> TERMINAL_STATES = Set.of("READY_TO_SERVE", "COMPLETED", "CANCELLED", "FAILED");

    private final WorkflowExecutionSupport workflow;
    private final List<OrderFulfillmentStep<OrderConfirmationContext>> confirmationSteps;
    private final OrderReadyNotificationStep readyNotificationStep;
    private final CancelOrderFulfillmentStep cancelOrderFulfillmentStep;
    private final PublishOrderCancellationFollowUpStep publishOrderCancellationFollowUpStep;

    public OrderFulfillmentMediator(
            WorkflowExecutionSupport workflow,
            List<OrderFulfillmentStep<OrderConfirmationContext>> confirmationSteps,
            OrderReadyNotificationStep readyNotificationStep,
            CancelOrderFulfillmentStep cancelOrderFulfillmentStep,
            PublishOrderCancellationFollowUpStep publishOrderCancellationFollowUpStep
    ) {
        this.workflow = workflow;
        this.confirmationSteps = List.copyOf(confirmationSteps);
        this.readyNotificationStep = readyNotificationStep;
        this.cancelOrderFulfillmentStep = cancelOrderFulfillmentStep;
        this.publishOrderCancellationFollowUpStep = publishOrderCancellationFollowUpStep;
    }

    @Transactional
    public void handleOrderConfirmed(EventEnvelope event) {
        ensureEventType(event, ServiceEventTypes.ORDER_CONFIRMED);
        UUID orderId = UUID.fromString(event.metadata().aggregateId());
        if (!workflow.startOrResume(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId.toString(), "ORDER_CONFIRMED_RECEIVED", event,
                Map.of("orderId", orderId.toString()), TERMINAL_STATES)) {
            return;
        }
        OrderConfirmationContext context = new OrderConfirmationContext(orderId, event);
        try {
            Map<String, Object> lastPayload = Map.of("orderId", orderId.toString());
            for (OrderFulfillmentStep<OrderConfirmationContext> step : confirmationSteps) {
                lastPayload = step.execute(context);
                workflow.recordStep(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId.toString(), step.name(),
                        WorkflowStepStatus.COMPLETED, event, null, lastPayload);
                workflow.transition(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId.toString(), step.name(), event, lastPayload);
            }
        } catch (RuntimeException exception) {
            workflow.fail(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId.toString(), "ORDER_CONFIRMATION_STEP", event, exception,
                    Map.of("orderId", orderId.toString()));
            throw exception;
        }
    }

    @Transactional
    public void handleDishStatusChanged(EventEnvelope event) {
        ensureEventType(event, ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED);
        String orderId = string(event.payload(), "orderId", null);
        if (orderId == null || !isReadyStatus(string(event.payload(), "status", ""))) {
            return;
        }
        WorkflowInstance instance = workflow.find(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId).orElse(null);
        if (instance == null || TERMINAL_STATES.contains(instance.state())) {
            return;
        }
        Set<String> expected = stringSet(instance.payload().get("expectedOrderItemIds"));
        Set<String> ready = stringSet(instance.payload().get("readyOrderItemIds"));
        String orderItemId = string(event.payload(), "orderItemId", null);
        if (orderItemId != null) {
            ready.add(orderItemId);
        }
        Map<String, Object> payload = Map.of(
                "orderId", orderId,
                "expectedOrderItemIds", List.copyOf(expected),
                "readyOrderItemIds", List.copyOf(ready)
        );
        if (!expected.isEmpty() && ready.containsAll(expected)) {
            readyNotificationStep.execute(orderId, event);
            workflow.recordStep(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "ORDER_READY_NOTIFICATION_REQUESTED",
                    WorkflowStepStatus.COMPLETED, event, null, payload);
            workflow.transition(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "READY_TO_SERVE", event, payload);
        } else {
            workflow.transition(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "AWAITING_DISHES", event, payload);
        }
    }

    @Transactional
    public void handleOrderCancelled(EventEnvelope event) {
        ensureEventType(event, ServiceEventTypes.ORDER_CANCELLED);
        String orderId = event.metadata().aggregateId();
        OrderCancellationContext context = new OrderCancellationContext(
                orderId,
                event,
                string(event.payload(), "reason", "Order cancelled")
        );
        Map<String, Object> payload = cancelOrderFulfillmentStep.execute(context);
        workflow.startOrResume(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "CANCELLATION_RECEIVED", event, payload, Set.of("CANCELLED"));
        publishOrderCancellationFollowUpStep.execute(context);
        workflow.recordStep(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "ORDER_CANCELLATION_FOLLOW_UP_REQUESTED",
                WorkflowStepStatus.COMPENSATED, event, null, payload);
        workflow.transition(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, "CANCELLED", event, payload);
    }

    @Transactional
    public void replayOrderFulfillment(String orderId, String reason) {
        workflow.requestReplay(WORKFLOW_TYPE, AGGREGATE_TYPE, orderId, reason);
    }

    private Set<String> stringSet(Object value) {
        Set<String> values = new LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
        }
        return values;
    }

    private boolean isReadyStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return Set.of("READY", "READY_TO_SERVE", "SERVED").contains(normalized);
    }

    private void ensureEventType(EventEnvelope event, String expectedType) {
        if (!expectedType.equals(event.metadata().eventType())) {
            throw new ConflictException("Expected " + expectedType + " but received " + event.metadata().eventType() + ".");
        }
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
