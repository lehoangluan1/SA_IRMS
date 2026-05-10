package SA.irms.reservation.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.reservation.application.port.out.ReservationLifecycleCommandPort;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationAutomationService {
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final SharedIdentityDirectoryPort identityDirectoryPort;
    private final AuditRecorder auditService;
    private final ReservationNotificationCoordinator reservationNotificationCoordinator;
    private final ReservationWaitlistService reservationWaitlistService;
    private final ReservationTableAssignmentService tableAssignmentService;
    private final ReservationLifecycleCommandPort lifecycleCommandPort;
    private final Clock clock;

    public ReservationAutomationService(
            SharedIdentityPolicyPort identityPolicyPort,
            SharedIdentityDirectoryPort identityDirectoryPort,
            AuditRecorder auditService,
            ReservationNotificationCoordinator reservationNotificationCoordinator,
            ReservationWaitlistService reservationWaitlistService,
            ReservationTableAssignmentService tableAssignmentService,
            ReservationLifecycleCommandPort lifecycleCommandPort,
            Clock clock
    ) {
        this.identityPolicyPort = identityPolicyPort;
        this.identityDirectoryPort = identityDirectoryPort;
        this.auditService = auditService;
        this.reservationNotificationCoordinator = reservationNotificationCoordinator;
        this.reservationWaitlistService = reservationWaitlistService;
        this.tableAssignmentService = tableAssignmentService;
        this.lifecycleCommandPort = lifecycleCommandPort;
        this.clock = clock;
    }

    @Transactional
    public void processReservationTimers() {
        reservationWaitlistService.expireOverdueWaitlistEntries();
        expireOverdueReservations();
        queueUpcomingCheckInReminders();
    }

    private void expireOverdueReservations() {
        int graceMinutes = identityPolicyPort.getPolicySnapshot().seatingPolicy().reservationGraceMinutes();
        Instant cutoff = Instant.now(clock).minusSeconds((long) graceMinutes * 60L);
        UUID actorUserId = resolveAutomationActorUserId();
        List<UUID> reservationIds = lifecycleCommandPort.loadOverdueReservationIds(cutoff);
        for (UUID reservationId : reservationIds) {
            lifecycleCommandPort.cancelExpiredReservation(reservationId, "Arrival window expired");
            tableAssignmentService.releaseReservationTable(reservationId);
            auditService.record(
                    actorUserId,
                    "reservation.auto_cancelled",
                    "Reservation",
                    reservationId.toString(),
                    "system:auto-cancel",
                    "Arrival window expired.",
                    true,
                    null,
                    Map.of("status", "confirmed"),
                    Map.of("status", "cancelled")
            );
            reservationNotificationCoordinator.queueStaffReservationNotification(
                    reservationId,
                    "reservation_cancellation",
                    "RESERVATION_CANCELLED",
                    "Reservation expired",
                    "A reservation was cancelled because the arrival window expired."
            );
        }
    }

    private void queueUpcomingCheckInReminders() {
        Instant now = Instant.now(clock);
        Instant windowEnd = now.plusSeconds(15 * 60L);
        lifecycleCommandPort.loadUpcomingReminderReservationIds(now, windowEnd, now.minusSeconds(3600))
                .forEach(reservationId -> reservationNotificationCoordinator.queueStaffReservationNotification(
                        reservationId,
                        "reservation_check_in_time",
                        "RESERVATION_CHECKIN_TIME",
                        "Reservation arriving soon",
                        "A reservation is approaching its check-in time."
                ));
    }

    private UUID resolveAutomationActorUserId() {
        return identityDirectoryPort.findFirstActiveUserIdByRolePriority(List.of("admin", "manager"))
                .orElseThrow(() -> new ConflictException("No active manager account is available for automated reservation processing."));
    }
}
