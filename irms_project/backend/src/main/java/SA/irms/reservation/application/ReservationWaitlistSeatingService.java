package SA.irms.reservation.application;

import SA.irms.reservation.application.query.WaitlistRow;
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

@Service
public class ReservationWaitlistSeatingService {
    private final ReservationWaitlistCommandPort waitlistCommandPort;
    private final AuditRecorder auditService;
    private final WaitlistQueryRepository waitlistReadService;
    private final ReservationWaitlistExpiryService waitlistExpiryService;
    private final ReservationSessionServerResolver sessionServerResolver;

    public ReservationWaitlistSeatingService(
            ReservationWaitlistCommandPort waitlistCommandPort,
            AuditRecorder auditService,
            WaitlistQueryRepository waitlistReadService,
            ReservationWaitlistExpiryService waitlistExpiryService,
            ReservationSessionServerResolver sessionServerResolver
    ) {
        this.waitlistCommandPort = waitlistCommandPort;
        this.auditService = auditService;
        this.waitlistReadService = waitlistReadService;
        this.waitlistExpiryService = waitlistExpiryService;
        this.sessionServerResolver = sessionServerResolver;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView seatWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        WaitlistRow row = waitlistReadService.loadWaitlistRow(waitlistEntryId);
        if (!"notified".equals(row.status())) {
            throw new ConflictException("Only notified waitlist entries can be seated.");
        }
        if (waitlistExpiryService.isWaitlistHoldExpired(row)) {
            waitlistExpiryService.expireWaitlistEntry(waitlistEntryId, correlationId, "Hold expired before the guest could be seated.");
            throw new ConflictException("The waitlist hold window has already expired.");
        }
        UUID tableId = waitlistReadService.requireAvailableTableIdForWaitlist(row.partySize());
        UUID sessionId = waitlistCommandPort.createTableSession(
                tableId,
                sessionServerResolver.resolveSessionServer(actor),
                row.partySize(),
                correlationId
        );
        waitlistCommandPort.markTableOccupied(tableId);
        waitlistCommandPort.markWaitlistSeated(waitlistEntryId);
        waitlistCommandPort.createWaitlistTableAssignment(waitlistEntryId, tableId, sessionId);
        SA.irms.reservation.application.view.ReservationViews.WaitlistView view = waitlistReadService.findWaitlistEntry(waitlistEntryId);
        auditService.record(actor.userId(), "waitlist.seated", "WaitlistEntry", waitlistEntryId.toString(), correlationId, null, false,
                request.remoteIp(), Map.of("status", "notified"), Map.of("status", "seated", "tableId", tableId.toString(), "sessionId", sessionId.toString()));
        return view;
    }
}
