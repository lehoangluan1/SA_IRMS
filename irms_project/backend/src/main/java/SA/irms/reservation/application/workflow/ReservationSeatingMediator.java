package SA.irms.reservation.application.workflow;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.events.EventEnvelope;
import SA.irms.reservation.application.events.ReservationSeatedEvent;
import SA.irms.reservation.application.events.WaitlistUpdatedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Service
public class ReservationSeatingMediator {
    private static final String RESERVATION_WORKFLOW = "ReservationSeating";
    private static final String WAITLIST_WORKFLOW = "WaitlistSeating";

    private final ValidateReservationTransitionStep validateReservationTransitionStep;
    private final AssignTableStep assignTableStep;
    private final SeatReservationStep seatReservationStep;
    private final UpdateWaitlistStep updateWaitlistStep;
    private final RequestReservationNotificationStep requestReservationNotificationStep;
    private final PersistReservationWorkflowStep persistReservationWorkflowStep;
    private final DomainEventPublisher outboxPublisher;

    public ReservationSeatingMediator(
            ValidateReservationTransitionStep validateReservationTransitionStep,
            AssignTableStep assignTableStep,
            SeatReservationStep seatReservationStep,
            UpdateWaitlistStep updateWaitlistStep,
            RequestReservationNotificationStep requestReservationNotificationStep,
            PersistReservationWorkflowStep persistReservationWorkflowStep,
            DomainEventPublisher outboxPublisher
    ) {
        this.validateReservationTransitionStep = validateReservationTransitionStep;
        this.assignTableStep = assignTableStep;
        this.seatReservationStep = seatReservationStep;
        this.updateWaitlistStep = updateWaitlistStep;
        this.requestReservationNotificationStep = requestReservationNotificationStep;
        this.persistReservationWorkflowStep = persistReservationWorkflowStep;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public void seatReservation(UUID reservationId, UUID tableId, String correlationId) {
        EventEnvelope workflowEvent = workflowEvent("Reservation", reservationId.toString(), correlationId, Map.of(
                "reservationId", reservationId.toString(),
                "tableId", tableId.toString()
        ));
        Map<String, Object> payload = validateReservationTransitionStep.validate(reservationId, tableId);
        persistReservationWorkflowStep.start(RESERVATION_WORKFLOW, "Reservation", reservationId.toString(), "SEATING_REQUESTED", workflowEvent, payload);
        try {
            assignTableStep.assign(reservationId, tableId);
            seatReservationStep.seat(reservationId, tableId);
            outboxPublisher.publish(new ReservationSeatedEvent(reservationId.toString(), payload), correlationId, null);
            requestReservationNotificationStep.notifySeated(reservationId, workflowEvent);
            persistReservationWorkflowStep.complete(RESERVATION_WORKFLOW, "Reservation", reservationId.toString(), "SEATED", workflowEvent, payload);
        } catch (RuntimeException exception) {
            persistReservationWorkflowStep.fail(RESERVATION_WORKFLOW, "Reservation", reservationId.toString(), workflowEvent, exception, payload);
            throw exception;
        }
    }

    @Transactional
    public void handleWaitlistUpdated(EventEnvelope event) {
        persistReservationWorkflowStep.start(WAITLIST_WORKFLOW, "WaitlistEntry", event.metadata().aggregateId(), "WAITLIST_UPDATED", event, event.payload());
        try {
            requestReservationNotificationStep.notifyWaitlist(UUID.fromString(event.metadata().aggregateId()), event);
            persistReservationWorkflowStep.complete(WAITLIST_WORKFLOW, "WaitlistEntry", event.metadata().aggregateId(), "WAITLIST_NOTIFIED", event, event.payload());
        } catch (RuntimeException exception) {
            persistReservationWorkflowStep.fail(WAITLIST_WORKFLOW, "WaitlistEntry", event.metadata().aggregateId(), event, exception, event.payload());
            throw exception;
        }
    }

    @Transactional
    public void updateWaitlist(UUID waitlistEntryId, String status, String correlationId) {
        EventEnvelope workflowEvent = workflowEvent("WaitlistEntry", waitlistEntryId.toString(), correlationId, Map.of(
                "waitlistEntryId", waitlistEntryId.toString(),
                "status", status
        ));
        persistReservationWorkflowStep.start(WAITLIST_WORKFLOW, "WaitlistEntry", waitlistEntryId.toString(), "WAITLIST_UPDATE_REQUESTED", workflowEvent, workflowEvent.payload());
        try {
            updateWaitlistStep.execute(waitlistEntryId, status);
            outboxPublisher.publish(new WaitlistUpdatedEvent(waitlistEntryId.toString(), Map.of(
                    "waitlistEntryId", waitlistEntryId.toString(),
                    "status", status
            )), correlationId, null);
            persistReservationWorkflowStep.complete(WAITLIST_WORKFLOW, "WaitlistEntry", waitlistEntryId.toString(), "WAITLIST_UPDATED", workflowEvent, workflowEvent.payload());
        } catch (RuntimeException exception) {
            persistReservationWorkflowStep.fail(WAITLIST_WORKFLOW, "WaitlistEntry", waitlistEntryId.toString(), workflowEvent, exception, workflowEvent.payload());
            throw exception;
        }
    }

    private EventEnvelope workflowEvent(String aggregateType, String aggregateId, String correlationId, Map<String, Object> payload) {
        String safeCorrelationId = correlationId == null || correlationId.isBlank() ? "reservation-workflow-" + aggregateId : correlationId;
        SA.irms.common.events.EventMetadata metadata = new SA.irms.common.events.EventMetadata(
                UUID.randomUUID(),
                "ReservationWorkflowCommand",
                1,
                aggregateType,
                aggregateId,
                java.time.Instant.now(),
                "reservation-service",
                safeCorrelationId,
                safeCorrelationId,
                "ReservationWorkflowCommand:" + aggregateType + ":" + aggregateId + ":" + safeCorrelationId
        );
        return new EventEnvelope(metadata, payload);
    }
}
