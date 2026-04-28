package SA.irms.identity.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface IdentityPolicyRepositoryPort {
    PolicySnapshot getPolicySnapshot();

    void updateSeatingPolicy(UUID policyId, int maxHoldMinutes, int reservationGraceMinutes);

    void updatePreparationPolicy(UUID policyId, int rushThresholdMin);

    void updateExpediteRule(UUID ruleId, int lateThresholdMin);

    void updateAuthorizationRefundWindow(UUID policyId, int refundWindowHours);

    BranchRow findDefaultBranch();

    record AuthorizationPolicy(
            UUID policyId,
            String policyKey,
            boolean requiresReasonForOverride,
            BigDecimal maxRefundLimit,
            int refundWindowHours,
            int sessionIdleTimeoutMinutes,
            int sessionAbsoluteTimeoutHours
    ) {
    }

    record SeatingPolicy(UUID policyId, int maxHoldMinutes, BigDecimal walkInBias, int reservationGraceMinutes) {
    }

    record PricingPolicy(UUID policyId, String name, BigDecimal serviceChargeRate, boolean supportsHappyHour) {
    }

    record TaxPolicy(UUID policyId, String name, BigDecimal taxRate, BigDecimal serviceFeeRate, boolean tipEditable) {
    }

    record PreparationPolicy(UUID policyId, boolean groupByStation, boolean supportsBatching, int rushThresholdMin) {
    }

    record ExpediteRule(UUID ruleId, int vipBoost, int lateThresholdMin, boolean requiresManagerApproval) {
    }

    record PolicySnapshot(
            AuthorizationPolicy authorizationPolicy,
            SeatingPolicy seatingPolicy,
            PricingPolicy pricingPolicy,
            TaxPolicy taxPolicy,
            PreparationPolicy preparationPolicy,
            ExpediteRule expediteRule
    ) {
    }

    record BranchRow(UUID branchId, String code, String name, String timezone) {
    }
}
