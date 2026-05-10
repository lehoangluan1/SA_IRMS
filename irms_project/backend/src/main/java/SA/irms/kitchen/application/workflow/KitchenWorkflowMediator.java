package SA.irms.kitchen.application.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.kitchen.application.port.out.KitchenTicketWorkflowCommandPort;
import SA.irms.kitchen.domain.KitchenTicketState;
import SA.irms.kitchen.domain.service.KitchenDeadlinePolicy;
import SA.irms.kitchen.domain.service.KitchenTicketTransitionPolicy;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.EventMetadata;
import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.common.workflow.WorkflowExecutionSupport;
import SA.irms.common.workflow.WorkflowStepStatus;

@Service
public class KitchenWorkflowMediator {
    private static final String WORKFLOW_TYPE = "KitchenWorkflow";
    private static final String AGGREGATE_TYPE = "KitchenTicket";
    private static final Set<String> TERMINAL_STATES = Set.of("SERVED", "CANCELLED", "FAILED");

    private final KitchenTicketWorkflowCommandPort ticketCommandPort;
    private final DomainEventPublisher outboxPublisher;
    private final WorkflowExecutionSupport workflow;
    private final KitchenTicketTransitionPolicy transitionPolicy;
    private final KitchenDeadlinePolicy deadlinePolicy;
    private final KitchenReadyNotificationStep readyNotificationStep;
    private final List<KitchenWorkflowStep<KitchenTicketTransitionContext>> transitionSteps;

    public KitchenWorkflowMediator(
            KitchenTicketWorkflowCommandPort ticketCommandPort,
            DomainEventPublisher outboxPublisher,
            WorkflowExecutionSupport workflow,
            KitchenTicketTransitionPolicy transitionPolicy,
            KitchenDeadlinePolicy deadlinePolicy,
            KitchenReadyNotificationStep readyNotificationStep,
            List<KitchenWorkflowStep<KitchenTicketTransitionContext>> transitionSteps
    ) {
        this.ticketCommandPort = ticketCommandPort;
        this.outboxPublisher = outboxPublisher;
        this.workflow = workflow;
        this.transitionPolicy = transitionPolicy;
        this.deadlinePolicy = deadlinePolicy;
        this.readyNotificationStep = readyNotificationStep;
        this.transitionSteps = List.copyOf(transitionSteps);
    }

    @Transactional
    public void handleDishStatusChanged(EventEnvelope envelope) {
        readyNotificationStep.execute(envelope);
    }

    @Transactional
    public Instant transitionTicket(UUID ticketId, KitchenTicketState currentState, KitchenTicketState targetState,
                                    int preparationMinutes, int itemCount, int priority, boolean peakHour) {
        transitionPolicy.ensureValidTransition(currentState, targetState);
        Instant deadline = deadlinePolicy.calculateDeadline(preparationMinutes, itemCount, priority, peakHour);
        EventEnvelope syntheticWorkflowEvent = workflowEvent(ticketId.toString(), "KitchenTicketTransition", Map.of(
                "ticketId", ticketId.toString(),
                "currentState", currentState.name(),
                "targetState", targetState.name()
        ));
        KitchenTicketTransitionContext context = new KitchenTicketTransitionContext(
                ticketId,
                currentState,
                targetState,
                preparationMinutes,
                itemCount,
                priority,
                peakHour,
                deadline,
                syntheticWorkflowEvent
        );
        workflow.startOrResume(WORKFLOW_TYPE, AGGREGATE_TYPE, ticketId.toString(), currentState.name(), syntheticWorkflowEvent,
                Map.of("ticketId", ticketId.toString()), TERMINAL_STATES);
        try {
            for (KitchenWorkflowStep<KitchenTicketTransitionContext> step : transitionSteps) {
                step.execute(context);
                workflow.recordStep(WORKFLOW_TYPE, AGGREGATE_TYPE, ticketId.toString(), step.name(),
                        WorkflowStepStatus.COMPLETED, syntheticWorkflowEvent, null, Map.of("ticketId", ticketId.toString()));
            }
            workflow.transition(WORKFLOW_TYPE, AGGREGATE_TYPE, ticketId.toString(), targetState.name(), syntheticWorkflowEvent, Map.of(
                    "ticketId", ticketId.toString(),
                    "deadline", deadline.toString(),
                    "priority", priority,
                    "itemCount", itemCount
            ));
            return deadline;
        } catch (RuntimeException exception) {
            workflow.fail(WORKFLOW_TYPE, AGGREGATE_TYPE, ticketId.toString(), "KITCHEN_TRANSITION", syntheticWorkflowEvent, exception,
                    Map.of("ticketId", ticketId.toString(), "targetState", targetState.name()));
            throw exception;
        }
    }

    public Instant calculateDeadline(int preparationMinutes, int itemCount, int priority, boolean peakHour) {
        return deadlinePolicy.calculateDeadline(preparationMinutes, itemCount, priority, peakHour);
    }

    @Transactional
    public void detectDelay(UUID ticketId, Instant deadline, String correlationId) {
        if (deadlinePolicy.isDelayed(deadline)) {
            outboxPublisher.publish("NotificationRequested", 1, "Notification", ticketId.toString(), Map.of(
                    "channel", "in_app",
                    "type", "kitchen_delay",
                    "title", "Kitchen delay",
                    "body", "Kitchen ticket " + ticketId + " is delayed.",
                    "priority", "high"
            ), correlationId, null, null);
        }
    }

    @Transactional
    public void handleOrderCancelled(EventEnvelope envelope) {
        UUID orderId = UUID.fromString(envelope.metadata().aggregateId());
        String reason = String.valueOf(envelope.payload().getOrDefault("reason", "Order cancelled"));
        ticketCommandPort.cancelOpenTicketsForOrder(orderId, reason);
        workflow.startOrResume(WORKFLOW_TYPE, "Order", orderId.toString(), "ORDER_CANCELLATION_RECEIVED", envelope,
                Map.of("orderId", orderId.toString(), "reason", reason), Set.of("CANCELLED"));
        workflow.transition(WORKFLOW_TYPE, "Order", orderId.toString(), "CANCELLED", envelope,
                Map.of("orderId", orderId.toString(), "reason", reason));
    }

    private EventEnvelope workflowEvent(String aggregateId, String eventType, Map<String, Object> payload) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID(),
                eventType,
                1,
                AGGREGATE_TYPE,
                aggregateId,
                Instant.now(),
                "kitchen-service",
                "kitchen-workflow-" + aggregateId,
                "kitchen-workflow-" + aggregateId,
                eventType + ":" + aggregateId
        );
        return new EventEnvelope(metadata, payload);
    }
}
