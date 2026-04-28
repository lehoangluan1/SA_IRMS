package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.PricingPolicy;

@Repository
class PricingPolicyLoader {
    private final JdbcClient jdbcClient;

    PricingPolicyLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    PricingPolicy load() {
        return jdbcClient.sql("""
                        select policy_id, name, service_charge_rate, supports_happy_hour
                        from pricing_policies
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """)
                .query((rs, rowNum) -> new PricingPolicy(
                        rs.getObject("policy_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("service_charge_rate"),
                        rs.getBoolean("supports_happy_hour")
                ))
                .single();
    }
}
