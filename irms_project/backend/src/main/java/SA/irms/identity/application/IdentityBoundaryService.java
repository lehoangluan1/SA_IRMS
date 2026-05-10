package SA.irms.identity.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.identity.application.port.out.IdentityDirectoryRepositoryPort;
import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort;
import SA.irms.identity.application.port.out.IdentitySessionRepositoryPort;
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.SessionPrincipal;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.identity.SharedIdentitySessionPort;

@Service
public class IdentityBoundaryService implements SharedIdentitySessionPort, SharedIdentityPolicyPort, SharedIdentityDirectoryPort {
    private final IdentitySessionRepositoryPort identitySessionRepository;
    private final IdentityPolicyRepositoryPort identityPolicyRepository;
    private final IdentityDirectoryRepositoryPort identityDirectoryRepository;

    public IdentityBoundaryService(
            IdentitySessionRepositoryPort identitySessionRepository,
            IdentityPolicyRepositoryPort identityPolicyRepository,
            IdentityDirectoryRepositoryPort identityDirectoryRepository
    ) {
        this.identitySessionRepository = identitySessionRepository;
        this.identityPolicyRepository = identityPolicyRepository;
        this.identityDirectoryRepository = identityDirectoryRepository;
    }

    @Override
    public Optional<SessionPrincipal> findActiveSessionByTokenHash(String tokenHash) {
        return identitySessionRepository.findActiveSessionByTokenHash(tokenHash).map(this::toSharedSessionPrincipal);
    }

    @Override
    @Transactional
    public void touchSession(UUID sessionId, Instant lastActivityAt) {
        identitySessionRepository.touchSession(sessionId, lastActivityAt);
    }

    @Override
    public PolicySnapshot getPolicySnapshot() {
        return toSharedPolicySnapshot(identityPolicyRepository.getPolicySnapshot());
    }

    @Override
    public BranchView findDefaultBranch() {
        IdentityPolicyRepositoryPort.BranchRow branch = identityPolicyRepository.findDefaultBranch();
        return new BranchView(branch.branchId(), branch.code(), branch.name(), branch.timezone());
    }

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        return identityDirectoryRepository.findDisplayNames(userIds);
    }

    @Override
    public Optional<UUID> findFirstActiveUserIdByRolePriority(List<String> rolePriority) {
        return identityDirectoryRepository.findFirstActiveUserIdByRolePriority(rolePriority);
    }

    private SessionPrincipal toSharedSessionPrincipal(IdentitySessionRepositoryPort.SessionPrincipal principal) {
        return new SessionPrincipal(
                principal.sessionId(),
                principal.userId(),
                principal.username(),
                principal.displayName(),
                principal.roles(),
                principal.permissions(),
                principal.expiresAt(),
                principal.lastActivityAt()
        );
    }

    private PolicySnapshot toSharedPolicySnapshot(IdentityPolicyRepositoryPort.PolicySnapshot snapshot) {
        return new PolicySnapshot(
                new PolicySnapshot.AuthorizationPolicy(
                        snapshot.authorizationPolicy().policyId(),
                        snapshot.authorizationPolicy().policyKey(),
                        snapshot.authorizationPolicy().requiresReasonForOverride(),
                        snapshot.authorizationPolicy().maxRefundLimit(),
                        snapshot.authorizationPolicy().refundWindowHours(),
                        snapshot.authorizationPolicy().sessionIdleTimeoutMinutes(),
                        snapshot.authorizationPolicy().sessionAbsoluteTimeoutHours()
                ),
                new PolicySnapshot.SeatingPolicy(
                        snapshot.seatingPolicy().policyId(),
                        snapshot.seatingPolicy().maxHoldMinutes(),
                        snapshot.seatingPolicy().walkInBias(),
                        snapshot.seatingPolicy().reservationGraceMinutes()
                ),
                new PolicySnapshot.PricingPolicy(
                        snapshot.pricingPolicy().policyId(),
                        snapshot.pricingPolicy().name(),
                        snapshot.pricingPolicy().serviceChargeRate(),
                        snapshot.pricingPolicy().supportsHappyHour()
                ),
                new PolicySnapshot.TaxPolicy(
                        snapshot.taxPolicy().policyId(),
                        snapshot.taxPolicy().name(),
                        snapshot.taxPolicy().taxRate(),
                        snapshot.taxPolicy().serviceFeeRate(),
                        snapshot.taxPolicy().tipEditable()
                ),
                new PolicySnapshot.PreparationPolicy(
                        snapshot.preparationPolicy().policyId(),
                        snapshot.preparationPolicy().groupByStation(),
                        snapshot.preparationPolicy().supportsBatching(),
                        snapshot.preparationPolicy().rushThresholdMin()
                ),
                new PolicySnapshot.ExpediteRule(
                        snapshot.expediteRule().ruleId(),
                        snapshot.expediteRule().vipBoost(),
                        snapshot.expediteRule().lateThresholdMin(),
                        snapshot.expediteRule().requiresManagerApproval()
                )
        );
    }
}
