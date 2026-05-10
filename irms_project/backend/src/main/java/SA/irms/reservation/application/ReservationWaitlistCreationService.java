package SA.irms.reservation.application;

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
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationWaitlistCreationService {
    private final ReservationWaitlistCommandPort waitlistCommandPort;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final WaitlistQueryRepository waitlistReadService;

    public ReservationWaitlistCreationService(
            ReservationWaitlistCommandPort waitlistCommandPort,
            SharedIdentityPolicyPort identityPolicyPort,
            AuditRecorder auditService,
            WaitlistQueryRepository waitlistReadService
    ) {
        this.waitlistCommandPort = waitlistCommandPort;
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.waitlistReadService = waitlistReadService;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView createWaitlistEntry(SA.irms.reservation.application.command.ReservationCommands.WaitlistUpsert request, AuthenticatedUser actor, String correlationId, RequestMetadata requestMetadata) {
        if (request.party() < 1) {
            throw new ConflictException("Party size must be at least 1.");
        }
        BranchView branch = identityPolicyPort.findDefaultBranch();
        UUID waitlistEntryId = waitlistCommandPort.createWaitlistEntry(
                branch.branchId(),
                request.name(),
                request.phone(),
                request.email(),
                request.party(),
                waitlistReadService.estimateQuotedWaitMinutes(),
                request.notes()
        );
        SA.irms.reservation.application.view.ReservationViews.WaitlistView waitlistView = waitlistReadService.findWaitlistEntry(waitlistEntryId);
        auditService.record(actor.userId(), "waitlist.created", "WaitlistEntry", waitlistEntryId.toString(), correlationId, null, false,
                requestMetadata.remoteIp(), Map.of(), Map.of(
                        "name", request.name(),
                        "party", request.party(),
                        "status", "waiting",
                        "tablesAvailableNow", waitlistView.tablesAvailableNow()
                ));
        return waitlistView;
    }

    @Transactional
    public void createWaitlistFromReservation(BranchView branch, String guest, String phone, int party, String notes, UUID reservationId) {
        waitlistCommandPort.createWaitlistEntry(
                branch.branchId(),
                guest,
                phone,
                null,
                party,
                waitlistReadService.estimateQuotedWaitMinutes(),
                notes == null ? "Auto-created from reservation " + reservationId + "." : notes
        );
    }
}
