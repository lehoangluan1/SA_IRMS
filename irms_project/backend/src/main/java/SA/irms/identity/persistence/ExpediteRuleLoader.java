package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.ExpediteRule;

@Repository
class ExpediteRuleLoader {
    private final JdbcClient jdbcClient;

    ExpediteRuleLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ExpediteRule load() {
        return jdbcClient.sql("""
                        select rule_id, vip_boost, late_threshold_min, requires_manager_approval
                        from expedite_rules
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """)
                .query((rs, rowNum) -> new ExpediteRule(
                        rs.getObject("rule_id", UUID.class),
                        rs.getInt("vip_boost"),
                        rs.getInt("late_threshold_min"),
                        rs.getBoolean("requires_manager_approval")
                ))
                .single();
    }
}
