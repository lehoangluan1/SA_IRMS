package SA.irms.identity.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.config.AppProperties;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.identity.application.command.IdentityCommands;
import SA.irms.identity.audit.AuditService;
import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort;
import SA.irms.common.context.RequestMetadata;

@Service
public class SettingsService {
    private final IdentityPolicyRepositoryPort identityPolicyRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;

    public SettingsService(
            IdentityPolicyRepositoryPort identityPolicyRepository,
            AppProperties appProperties,
            AuditService auditService
    ) {
        this.identityPolicyRepository = identityPolicyRepository;
        this.appProperties = appProperties;
        this.auditService = auditService;
    }

    public Map<String, Object> loadSettings() {
        IdentityPolicyRepositoryPort.PolicySnapshot policies = identityPolicyRepository.getPolicySnapshot();
        IdentityPolicyRepositoryPort.BranchRow branch = identityPolicyRepository.findDefaultBranch();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("general", Map.of(
                "restaurantName", branch.name(),
                "timezone", branch.timezone(),
                "currency", appProperties.restaurant().currency(),
                "taxRate", policies.taxPolicy().taxRate(),
                "serviceFeeRate", policies.taxPolicy().serviceFeeRate()
        ));
        payload.put("operations", Map.of(
                "waitlistHoldMinutes", policies.seatingPolicy().maxHoldMinutes(),
                "reservationGraceMinutes", policies.seatingPolicy().reservationGraceMinutes(),
                "kitchenRushThresholdMinutes", policies.preparationPolicy().rushThresholdMin(),
                "kitchenLateThresholdMinutes", policies.expediteRule().lateThresholdMin(),
                "refundWindowHours", policies.authorizationPolicy().refundWindowHours()
        ));
        payload.put("sessionSecurity", Map.of(
                "sessionTimeoutHours", appProperties.security().sessionAbsoluteTimeoutHours(),
                "idleTimeoutMinutes", appProperties.security().sessionIdleTimeoutMinutes()
        ));
        return payload;
    }

    @Transactional
    public Map<String, Object> updateSettings(
            IdentityCommands.UpdateSettingsRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        validate(request);
        IdentityPolicyRepositoryPort.PolicySnapshot before = identityPolicyRepository.getPolicySnapshot();

        identityPolicyRepository.updateSeatingPolicy(
                before.seatingPolicy().policyId(),
                request.waitlistHoldMinutes(),
                request.reservationGraceMinutes()
        );
        identityPolicyRepository.updatePreparationPolicy(before.preparationPolicy().policyId(), request.kitchenRushThresholdMinutes());
        identityPolicyRepository.updateExpediteRule(before.expediteRule().ruleId(), request.kitchenLateThresholdMinutes());
        identityPolicyRepository.updateAuthorizationRefundWindow(before.authorizationPolicy().policyId(), request.refundWindowHours());

        IdentityPolicyRepositoryPort.PolicySnapshot after = identityPolicyRepository.getPolicySnapshot();
        auditService.record(
                actor.userId(),
                "settings.updated",
                "PolicySnapshot",
                "default",
                correlationId,
                "Operational settings updated.",
                true,
                httpServletRequest.remoteIp(),
                Map.of(
                        "waitlistHoldMinutes", before.seatingPolicy().maxHoldMinutes(),
                        "reservationGraceMinutes", before.seatingPolicy().reservationGraceMinutes(),
                        "kitchenRushThresholdMinutes", before.preparationPolicy().rushThresholdMin(),
                        "kitchenLateThresholdMinutes", before.expediteRule().lateThresholdMin(),
                        "refundWindowHours", before.authorizationPolicy().refundWindowHours()
                ),
                Map.of(
                        "waitlistHoldMinutes", after.seatingPolicy().maxHoldMinutes(),
                        "reservationGraceMinutes", after.seatingPolicy().reservationGraceMinutes(),
                        "kitchenRushThresholdMinutes", after.preparationPolicy().rushThresholdMin(),
                        "kitchenLateThresholdMinutes", after.expediteRule().lateThresholdMin(),
                        "refundWindowHours", after.authorizationPolicy().refundWindowHours()
                )
        );
        return loadSettings();
    }

    private void validate(IdentityCommands.UpdateSettingsRequest request) {
        if (request.waitlistHoldMinutes() < 1) {
            throw new ConflictException("Waitlist hold time must be at least 1 minute.");
        }
        if (request.reservationGraceMinutes() < 0) {
            throw new ConflictException("Reservation grace time cannot be negative.");
        }
        if (request.kitchenRushThresholdMinutes() < 0) {
            throw new ConflictException("Kitchen rush threshold cannot be negative.");
        }
        if (request.kitchenLateThresholdMinutes() < 0) {
            throw new ConflictException("Kitchen late threshold cannot be negative.");
        }
        if (request.refundWindowHours() < 1) {
            throw new ConflictException("Refund window must be at least 1 hour.");
        }
    }
}
