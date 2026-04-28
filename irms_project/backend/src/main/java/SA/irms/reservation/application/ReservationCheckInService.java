package SA.irms.reservation.application;

import SA.irms.reservation.application.query.ReservationRow;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.reservation.application.port.out.ReservationCheckInCommandPort;
import SA.irms.reservation.application.port.out.ReservationQueryRepository;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationCheckInService {
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final ReservationQueryRepository reservationReadService;
    private final ReservationTableAssignmentService tableAssignmentService;
    private final ReservationSessionServerResolver sessionServerResolver;
    private final ReservationCheckInCommandPort checkInCommandPort;
    private final Clock clock;

    public ReservationCheckInService(
            SharedIdentityPolicyPort identityPolicyPort,
            AuditRecorder auditService,
            ReservationQueryRepository reservationReadService,
            ReservationTableAssignmentService tableAssignmentService,
            ReservationSessionServerResolver sessionServerResolver,
            ReservationCheckInCommandPort checkInCommandPort,
            Clock clock
    ) {
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.reservationReadService = reservationReadService;
        this.tableAssignmentService = tableAssignmentService;
        this.sessionServerResolver = sessionServerResolver;
        this.checkInCommandPort = checkInCommandPort;
        this.clock = clock;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView checkIn(
            UUID reservationId,
            SA.irms.reservation.application.command.ReservationCommands.CheckInRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata requestMetadata
    ) {
        ReservationRow reservation = reservationReadService.loadReservationRow(reservationId);
        int actualPartySize = request != null && request.actualPartySize() != null ? request.actualPartySize() : reservation.partySize();
        if (actualPartySize < 1) {
            throw new ConflictException("Actual party size must be at least 1.");
        }
        int graceMinutes = identityPolicyPort.getPolicySnapshot().seatingPolicy().reservationGraceMinutes();
        boolean outsideGraceWindow = reservation.arrivalAt().plusSeconds((long) graceMinutes * 60L).isBefore(Instant.now(clock));
        boolean approveRecovery = request != null && Boolean.TRUE.equals(request.approveRecovery());
        if (outsideGraceWindow) {
            if (!approveRecovery) {
                throw new ConflictException("Manager approval is required because the reservation is outside the grace window.");
            }
            if (!actor.hasRole("manager") && !actor.hasRole("admin")) {
                throw new ConflictException("Only a manager or admin can approve a late-arrival recovery.");
            }
        }
        UUID tableId = tableAssignmentService.resolveCheckInTableId(reservationId, actualPartySize, request == null ? null : request.replacementTableId());
        tableAssignmentService.alignReservationAssignment(reservationId, tableId);
        UUID sessionId = checkInCommandPort.openReservationSession(
                reservationId,
                tableId,
                sessionServerResolver.resolveSessionServer(actor),
                actualPartySize,
                correlationId
        );
        checkInCommandPort.markTableOccupied(tableId);
        checkInCommandPort.markReservationSeated(reservationId, actualPartySize);
        checkInCommandPort.linkReservationAssignmentSession(reservationId, tableId, sessionId);
        auditService.record(
                actor.userId(),
                outsideGraceWindow ? "reservation.recovery.checked_in" : "reservation.checked_in",
                "Reservation",
                reservationId.toString(),
                correlationId,
                outsideGraceWindow ? "Late-arrival recovery approved." : null,
                false,
                requestMetadata.remoteIp(),
                Map.of("status", reservation.status()),
                Map.of("status", "seated", "tableId", tableId.toString(), "sessionId", sessionId.toString(), "partySize", actualPartySize)
        );
        return reservationReadService.findReservation(reservationId);
    }
}
