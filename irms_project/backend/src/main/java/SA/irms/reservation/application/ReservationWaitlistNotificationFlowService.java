package SA.irms.reservation.application;

import SA.irms.reservation.application.query.WaitlistRow;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.reservation.application.port.out.ReservationWaitlistCommandPort;
import SA.irms.reservation.application.port.out.WaitlistQueryRepository;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationWaitlistNotificationFlowService {
    private final ReservationWaitlistCommandPort waitlistCommandPort;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final ReservationNotificationCoordinator reservationNotificationCoordinator;
    private final WaitlistQueryRepository waitlistReadService;
    private final ReservationWaitlistExpiryService waitlistExpiryService;

    public ReservationWaitlistNotificationFlowService(
            ReservationWaitlistCommandPort waitlistCommandPort,
            SharedIdentityPolicyPort identityPolicyPort,
            AuditRecorder auditService,
            ReservationNotificationCoordinator reservationNotificationCoordinator,
            WaitlistQueryRepository waitlistReadService,
            ReservationWaitlistExpiryService waitlistExpiryService
    ) {
        this.waitlistCommandPort = waitlistCommandPort;
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.reservationNotificationCoordinator = reservationNotificationCoordinator;
        this.waitlistReadService = waitlistReadService;
        this.waitlistExpiryService = waitlistExpiryService;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView notifyWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        WaitlistRow row = waitlistReadService.loadWaitlistRow(waitlistEntryId);
        rejectExpiredHold(row, waitlistEntryId, correlationId);
        if (!List.of("waiting", "skipped", "expired").contains(row.status())) {
            throw new ConflictException("Only active waitlist entries can be notified.");
        }
        UUID suggestedTableId = waitlistReadService.requireAvailableTableIdForWaitlist(row.partySize());
        int maxHold = identityPolicyPort.getPolicySnapshot().seatingPolicy().maxHoldMinutes();
        waitlistCommandPort.markNotified(waitlistEntryId, maxHold);
        reservationNotificationCoordinator.sendNotification(null, waitlistEntryId, "Waitlist seat available", "A table is ready for your party.", "sms");
        SA.irms.reservation.application.view.ReservationViews.WaitlistView view = waitlistReadService.findWaitlistEntry(waitlistEntryId);
        auditService.record(actor.userId(), "waitlist.notified", "WaitlistEntry", waitlistEntryId.toString(), correlationId, null, false,
                request.remoteIp(), Map.of("status", row.status()), Map.of("status", "notified", "tableId", suggestedTableId.toString()));
        return view;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView skipWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        WaitlistRow row = waitlistReadService.loadWaitlistRow(waitlistEntryId);
        rejectExpiredHold(row, waitlistEntryId, correlationId);
        if (!"notified".equals(row.status())) {
            throw new ConflictException("Only notified waitlist entries can be marked as skipped.");
        }
        waitlistCommandPort.markSkipped(waitlistEntryId);
        SA.irms.reservation.application.view.ReservationViews.WaitlistView view = waitlistReadService.findWaitlistEntry(waitlistEntryId);
        auditService.record(actor.userId(), "waitlist.skipped", "WaitlistEntry", waitlistEntryId.toString(), correlationId,
                "No response within the hold window.", false, request.remoteIp(), Map.of("status", row.status()), Map.of("status", "skipped"));
        return view;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView prioritizeWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        WaitlistRow row = waitlistReadService.loadWaitlistRow(waitlistEntryId);
        rejectExpiredHold(row, waitlistEntryId, correlationId);
        if (!List.of("waiting", "notified", "skipped", "expired").contains(row.status())) {
            throw new ConflictException("Only active waitlist entries can be prioritized.");
        }
        int nextPriority = waitlistCommandPort.nextPriority();
        waitlistCommandPort.updatePriority(waitlistEntryId, nextPriority);
        SA.irms.reservation.application.view.ReservationViews.WaitlistView view = waitlistReadService.findWaitlistEntry(waitlistEntryId);
        auditService.record(actor.userId(), "waitlist.prioritized", "WaitlistEntry", waitlistEntryId.toString(), correlationId,
                "Front desk override applied to the waitlist order.", false, request.remoteIp(), Map.of("priority", row.priority()), Map.of("priority", nextPriority));
        return view;
    }

    private void rejectExpiredHold(WaitlistRow row, UUID waitlistEntryId, String correlationId) {
        if (waitlistExpiryService.isWaitlistHoldExpired(row)) {
            waitlistExpiryService.expireWaitlistEntry(waitlistEntryId, correlationId, "Hold expired before the guest could be seated.");
            throw new ConflictException("The waitlist hold window has already expired.");
        }
    }
}
