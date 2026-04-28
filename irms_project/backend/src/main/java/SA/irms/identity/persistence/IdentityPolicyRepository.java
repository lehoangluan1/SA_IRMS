package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort;

@Repository
public class IdentityPolicyRepository implements IdentityPolicyRepositoryPort {
    private final JdbcClient jdbcClient;
    private final AuthorizationPolicyLoader authorizationPolicyLoader;
    private final SeatingPolicyLoader seatingPolicyLoader;
    private final PricingPolicyLoader pricingPolicyLoader;
    private final TaxPolicyLoader taxPolicyLoader;
    private final PreparationPolicyLoader preparationPolicyLoader;
    private final ExpediteRuleLoader expediteRuleLoader;

    public IdentityPolicyRepository(
            JdbcClient jdbcClient,
            AuthorizationPolicyLoader authorizationPolicyLoader,
            SeatingPolicyLoader seatingPolicyLoader,
            PricingPolicyLoader pricingPolicyLoader,
            TaxPolicyLoader taxPolicyLoader,
            PreparationPolicyLoader preparationPolicyLoader,
            ExpediteRuleLoader expediteRuleLoader
    ) {
        this.jdbcClient = jdbcClient;
        this.authorizationPolicyLoader = authorizationPolicyLoader;
        this.seatingPolicyLoader = seatingPolicyLoader;
        this.pricingPolicyLoader = pricingPolicyLoader;
        this.taxPolicyLoader = taxPolicyLoader;
        this.preparationPolicyLoader = preparationPolicyLoader;
        this.expediteRuleLoader = expediteRuleLoader;
    }

    @Override
    public PolicySnapshot getPolicySnapshot() {
        return new PolicySnapshot(
                authorizationPolicyLoader.load(),
                seatingPolicyLoader.load(),
                pricingPolicyLoader.load(),
                taxPolicyLoader.load(),
                preparationPolicyLoader.load(),
                expediteRuleLoader.load()
        );
    }

    @Override
    public void updateSeatingPolicy(UUID policyId, int maxHoldMinutes, int reservationGraceMinutes) {
        jdbcClient.sql("""
                        update seating_policies
                        set max_hold_minutes = :maxHoldMinutes,
                            reservation_grace_minutes = :reservationGraceMinutes,
                            updated_at = now()
                        where policy_id = :policyId
                        """)
                .param("maxHoldMinutes", maxHoldMinutes)
                .param("reservationGraceMinutes", reservationGraceMinutes)
                .param("policyId", policyId)
                .update();
    }

    @Override
    public void updateAuthorizationRefundWindow(UUID policyId, int refundWindowHours) {
        jdbcClient.sql("""
                        update authorization_policies
                        set refund_window_hours = :refundWindowHours,
                            updated_at = now()
                        where policy_id = :policyId
                        """)
                .param("refundWindowHours", refundWindowHours)
                .param("policyId", policyId)
                .update();
    }

    @Override
    public void updatePreparationPolicy(UUID policyId, int rushThresholdMin) {
        jdbcClient.sql("""
                        update preparation_policies
                        set rush_threshold_min = :rushThresholdMin,
                            updated_at = now()
                        where policy_id = :policyId
                        """)
                .param("rushThresholdMin", rushThresholdMin)
                .param("policyId", policyId)
                .update();
    }

    @Override
    public void updateExpediteRule(UUID ruleId, int lateThresholdMin) {
        jdbcClient.sql("""
                        update expedite_rules
                        set late_threshold_min = :lateThresholdMin,
                            updated_at = now()
                        where rule_id = :ruleId
                        """)
                .param("lateThresholdMin", lateThresholdMin)
                .param("ruleId", ruleId)
                .update();
    }

    @Override
    public BranchRow findDefaultBranch() {
        return jdbcClient.sql("""
                        select branch_id, code, name, timezone
                        from branches
                        order by created_at
                        limit 1
                        """)
                .query((rs, rowNum) -> new BranchRow(
                        rs.getObject("branch_id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("timezone")
                ))
                .single();
    }
}
