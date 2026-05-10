package SA.irms.reservation.application;

import SA.irms.reservation.application.query.TableCandidate;
import SA.irms.reservation.application.support.ReservationTimeSupport;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.reservation.application.port.out.ReservationLifecycleCommandPort;
import SA.irms.reservation.application.port.out.ReservationQueryRepository;
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationLifecycleService {
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final ReservationWaitlistService reservationWaitlistService;
    private final ReservationNotificationCoordinator reservationNotificationCoordinator;
    private final ReservationQueryRepository reservationReadService;
    private final ReservationTableAssignmentService tableAssignmentService;
    private final ReservationCheckInService checkInService;
    private final ReservationLifecycleCommandPort lifecycleCommandPort;
    private final Clock clock;

    public ReservationLifecycleService(
            SharedIdentityPolicyPort identityPolicyPort,
            AuditRecorder auditService,
            ReservationWaitlistService reservationWaitlistService,
            ReservationNotificationCoordinator reservationNotificationCoordinator,
            ReservationQueryRepository reservationReadService,
            ReservationTableAssignmentService tableAssignmentService,
            ReservationCheckInService checkInService,
            ReservationLifecycleCommandPort lifecycleCommandPort,
            Clock clock
    ) {
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.reservationWaitlistService = reservationWaitlistService;
        this.reservationNotificationCoordinator = reservationNotificationCoordinator;
        this.reservationReadService = reservationReadService;
        this.tableAssignmentService = tableAssignmentService;
        this.checkInService = checkInService;
        this.lifecycleCommandPort = lifecycleCommandPort;
        this.clock = clock;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView createReservation(
            SA.irms.reservation.application.command.ReservationCommands.ReservationUpsert request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata requestMetadata
    ) {
        if (request.party() < 1) {
            throw new ConflictException("Party size must be at least 1.");
        }
        BranchView branch = identityPolicyPort.findDefaultBranch();
        UUID contactId = lifecycleCommandPort.createContact(request.guest(), request.phone(), request.email());
        Instant arrivalAt = ReservationTimeSupport.toArrivalInstant(request.date(), request.time());
        TableCandidate assignedTable;
        if (request.tableId() != null) {
            assignedTable = tableAssignmentService.loadCandidateTable(request.tableId())
                    .orElseThrow(() -> new NotFoundException("The selected table was not found."));
            tableAssignmentService.validateAssignedTable(assignedTable, request.party());
        } else {
            assignedTable = tableAssignmentService.findCandidateTables(request.party()).stream().findFirst().orElse(null);
        }
        String status = assignedTable == null ? "pending" : "confirmed";
        UUID reservationId = lifecycleCommandPort.createReservation(
                branch.branchId(),
                contactId,
                arrivalAt,
                request.party(),
                status,
                request.notes(),
                "confirmed".equals(status) ? Instant.now(clock) : null,
                arrivalAt.plusSeconds(15 * 60L)
        );
        if (assignedTable != null) {
            tableAssignmentService.reserveTableForReservation(reservationId, assignedTable.tableId());
        } else if (request.fallbackToWaitlist()) {
            reservationWaitlistService.createWaitlistFromReservation(branch, request.guest(), request.phone(), request.party(), request.notes(), reservationId);
        }
        auditService.record(actor.userId(), "reservation.created", "Reservation", reservationId.toString(), correlationId, null, false,
                requestMetadata.remoteIp(), Map.of(),
                Map.of("guest", request.guest(), "party", request.party(), "status", status, "waitlistFallback", request.fallbackToWaitlist()));
        return reservationReadService.findReservation(reservationId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView updateReservation(UUID reservationId, SA.irms.reservation.application.command.ReservationCommands.ReservationPatch request) {
        lifecycleCommandPort.updateReservation(reservationId, request.notes(), request.party());
        lifecycleCommandPort.updateContactNameForReservation(reservationId, request.guest());
        return reservationReadService.findReservation(reservationId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView confirmReservation(UUID reservationId) {
        UUID assignedTableId = tableAssignmentService.findAssignedTableId(reservationId)
                .orElseGet(() -> tableAssignmentService.findAvailableTableIdForReservation(reservationId).orElse(null));
        if (assignedTableId != null) {
            tableAssignmentService.reserveTableForReservation(reservationId, assignedTableId);
        }
        lifecycleCommandPort.confirmReservation(reservationId);
        return reservationReadService.findReservation(reservationId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView markNoShow(
            UUID reservationId,
            String correlationId,
            AuthenticatedUser actor,
            RequestMetadata requestMetadata
    ) {
        lifecycleCommandPort.markReservationNoShow(reservationId);
        tableAssignmentService.releaseReservationTable(reservationId);
        auditService.record(actor.userId(), "reservation.no_show", "Reservation", reservationId.toString(), correlationId, null, false,
                requestMetadata.remoteIp(), Map.of("status", "confirmed"), Map.of("status", "no_show"));
        reservationNotificationCoordinator.queueStaffReservationNotification(
                reservationId,
                "reservation_cancellation",
                "RESERVATION_CANCELLED",
                "Reservation released",
                "A reservation was released after being marked as no-show."
        );
        return reservationReadService.findReservation(reservationId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView checkIn(
            UUID reservationId,
            SA.irms.reservation.application.command.ReservationCommands.CheckInRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata requestMetadata
    ) {
        return checkInService.checkIn(reservationId, request, actor, correlationId, requestMetadata);
    }
}
