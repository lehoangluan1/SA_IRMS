package SA.irms.reservation.application;

import SA.irms.reservation.application.query.WaitlistRow;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.reservation.application.port.out.ReservationWaitlistCommandPort;
import SA.irms.common.identity.SharedIdentityDirectoryPort;

@Service
public class ReservationWaitlistExpiryService {
    private final ReservationWaitlistCommandPort waitlistCommandPort;
    private final SharedIdentityDirectoryPort identityDirectoryPort;
    private final AuditRecorder auditService;
    private final Clock clock;

    public ReservationWaitlistExpiryService(
            ReservationWaitlistCommandPort waitlistCommandPort,
            SharedIdentityDirectoryPort identityDirectoryPort,
            AuditRecorder auditService,
            Clock clock
    ) {
        this.waitlistCommandPort = waitlistCommandPort;
        this.identityDirectoryPort = identityDirectoryPort;
        this.auditService = auditService;
        this.clock = clock;
    }

    public boolean isWaitlistHoldExpired(WaitlistRow waitlistRow) {
        return "notified".equals(waitlistRow.status())
                && waitlistRow.holdExpiresAt() != null
                && waitlistRow.holdExpiresAt().isBefore(Instant.now(clock));
    }

    @Transactional
    public void expireOverdueWaitlistEntries() {
        waitlistCommandPort.loadExpiredNotifiedWaitlistIds().forEach(waitlistEntryId ->
                expireWaitlistEntry(waitlistEntryId, "system:waitlist-expire", "Hold expired before the guest could be seated."));
    }

    @Transactional
    public void expireWaitlistEntry(UUID waitlistEntryId, String correlationId, String reason) {
        boolean updated = waitlistCommandPort.expireNotifiedWaitlistEntry(waitlistEntryId);
        if (!updated) {
            return;
        }
        auditService.record(
                resolveAutomationActorUserId(),
                "waitlist.expired",
                "WaitlistEntry",
                waitlistEntryId.toString(),
                correlationId,
                reason,
                true,
                null,
                Map.of("status", "notified"),
                Map.of("status", "expired")
        );
    }

    private UUID resolveAutomationActorUserId() {
        return identityDirectoryPort.findFirstActiveUserIdByRolePriority(List.of("admin", "manager"))
                .orElseThrow(() -> new ConflictException("No active manager account is available for automated reservation processing."));
    }
}
