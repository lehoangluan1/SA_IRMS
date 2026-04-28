package SA.irms.identity.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.identity.application.port.out.IdentitySessionRepositoryPort;
import SA.irms.identity.application.port.out.IdentityUserRepository;

@Repository
public class IdentityRepository implements IdentityUserRepository, IdentitySessionRepositoryPort {
    private final IdentityUserAuthRepository userAuthRepository;
    private final IdentitySessionRepository sessionRepository;

    public IdentityRepository(IdentityUserAuthRepository userAuthRepository, IdentitySessionRepository sessionRepository) {
        this.userAuthRepository = userAuthRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Optional<IdentityUserRepository.UserAccount> findUserAccountByEmail(String email) {
        return userAuthRepository.findUserAccountByEmail(email).map(this::toUserAccount);
    }

    @Override
    public Optional<IdentityUserRepository.UserAccount> findUserAccountById(UUID userId) {
        return userAuthRepository.findUserAccountById(userId).map(this::toUserAccount);
    }

    @Override
    public Optional<IdentitySessionRepositoryPort.SessionPrincipal> findActiveSessionByTokenHash(String tokenHash) {
        return sessionRepository.findActiveSessionByTokenHash(tokenHash).map(this::toSessionPrincipal);
    }

    @Transactional
    @Override
    public void touchSession(UUID sessionId, Instant lastActivityAt) {
        sessionRepository.touchSession(sessionId, lastActivityAt);
    }

    @Transactional
    @Override
    public void terminateSession(UUID sessionId, Instant endedAt) {
        sessionRepository.terminateSession(sessionId, endedAt);
    }

    @Transactional
    @Override
    public void terminateActiveSessionsForUser(UUID userId, Instant endedAt) {
        sessionRepository.terminateActiveSessionsForUser(userId, endedAt);
    }

    @Transactional
    public void expireIdleSessions(Instant idleBefore, Instant now) {
        sessionRepository.expireIdleSessions(idleBefore, now);
    }

    @Transactional
    @Override
    public void updateLastLogin(UUID userId, Instant lastLoginAt) {
        userAuthRepository.updateLastLogin(userId, lastLoginAt);
    }

    @Transactional
    @Override
    public UUID createSession(UUID sessionId, UUID userId, String tokenHash, Instant startedAt, Instant expiresAt, String deviceId, String ipAddress) {
        return sessionRepository.createSession(sessionId, userId, tokenHash, startedAt, expiresAt, deviceId, ipAddress);
    }

    private IdentityUserRepository.UserAccount toUserAccount(UserAccount account) {
        return new IdentityUserRepository.UserAccount(
                account.userId(),
                account.username(),
                account.email(),
                account.displayName(),
                account.passwordHash(),
                account.status(),
                account.roles(),
                account.permissions()
        );
    }

    private IdentitySessionRepositoryPort.SessionPrincipal toSessionPrincipal(SessionPrincipal principal) {
        return new IdentitySessionRepositoryPort.SessionPrincipal(
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

    public record UserAccount(UUID userId, String username, String email, String displayName, String passwordHash, String status, Set<String> roles, Set<String> permissions) {
    }
    public record SessionPrincipal(UUID sessionId, UUID userId, String username, String displayName, Set<String> roles, Set<String> permissions, Instant expiresAt, Instant lastActivityAt) {
    }
    public record AuthorizationPolicy(UUID policyId, String policyKey, boolean requiresReasonForOverride, java.math.BigDecimal maxRefundLimit, int refundWindowHours, int sessionIdleTimeoutMinutes, int sessionAbsoluteTimeoutHours) {
    }
    public record SeatingPolicy(UUID policyId, int maxHoldMinutes, java.math.BigDecimal walkInBias, int reservationGraceMinutes) {
    }
    public record PricingPolicy(UUID policyId, String name, java.math.BigDecimal serviceChargeRate, boolean supportsHappyHour) {
    }
    public record TaxPolicy(UUID policyId, String name, java.math.BigDecimal taxRate, java.math.BigDecimal serviceFeeRate, boolean tipEditable) {
    }
    public record PreparationPolicy(UUID policyId, boolean groupByStation, boolean supportsBatching, int rushThresholdMin) {
    }
    public record ExpediteRule(UUID ruleId, int vipBoost, int lateThresholdMin, boolean requiresManagerApproval) {
    }
    public record PolicySnapshot(AuthorizationPolicy authorizationPolicy, SeatingPolicy seatingPolicy, PricingPolicy pricingPolicy, TaxPolicy taxPolicy, PreparationPolicy preparationPolicy, ExpediteRule expediteRule) {
    }
    public record StaffRow(UUID userId, String displayName, String email, String status, LocalDate hireDate, Set<String> roles, Set<String> permissions) {
    }
    public record RoleRow(UUID roleId, String name, String scope, String description, List<String> permissions) {
    }
    public record ShiftRow(UUID shiftAssignmentId, UUID userId, String displayName, String roleName, LocalDate shiftDate, LocalTime startAt, LocalTime endAt, String position, String zone, String status) {
    }
    public record AuditRow(UUID auditLogId, Instant recordedAt, UUID actorUserId, String actorName, Set<String> roles, String action, String entityType, String entityId, String correlationId, String reason, boolean followUp, String ipAddress, Map<String, Object> beforePayload, Map<String, Object> afterPayload) {
    }
    public record AuditDetailCommand(UUID detailId, String fieldName, String beforeValue, String afterValue) {
    }
    public record AuditCommand(UUID auditLogId, UUID sourceEventId, UUID actorUserId, String action, String entityType, String entityId, Instant recordedAt, String correlationId, String reason, boolean followUp, String ipAddress, Map<String, Object> beforePayload, Map<String, Object> afterPayload, List<AuditDetailCommand> details) {
    }
    public record ReportSnapshotRow(UUID snapshotId, String type, Instant periodStart, Instant periodEnd, Instant generatedAt, Map<String, Object> payload) {
    }
    public record BranchRow(UUID branchId, String code, String name, String timezone) {
    }
}
