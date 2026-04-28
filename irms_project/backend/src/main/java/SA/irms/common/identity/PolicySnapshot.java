package SA.irms.common.identity;

import java.math.BigDecimal;
import java.util.UUID;

public record PolicySnapshot(
        AuthorizationPolicy authorizationPolicy,
        SeatingPolicy seatingPolicy,
        PricingPolicy pricingPolicy,
        TaxPolicy taxPolicy,
        PreparationPolicy preparationPolicy,
        ExpediteRule expediteRule
) {
    public record AuthorizationPolicy(
            UUID policyId,
            String policyKey,
            boolean requiresReasonForOverride,
            BigDecimal maxRefundLimit,
            int refundWindowHours,
            int sessionIdleTimeoutMinutes,
            int sessionAbsoluteTimeoutHours
    ) {
    }

    public record SeatingPolicy(UUID policyId, int maxHoldMinutes, BigDecimal walkInBias, int reservationGraceMinutes) {
    }

    public record PricingPolicy(UUID policyId, String name, BigDecimal serviceChargeRate, boolean supportsHappyHour) {
    }

    public record TaxPolicy(UUID policyId, String name, BigDecimal taxRate, BigDecimal serviceFeeRate, boolean tipEditable) {
    }

    public record PreparationPolicy(UUID policyId, boolean groupByStation, boolean supportsBatching, int rushThresholdMin) {
    }

    public record ExpediteRule(UUID ruleId, int vipBoost, int lateThresholdMin, boolean requiresManagerApproval) {
    }
}
