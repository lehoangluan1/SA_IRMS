package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.TaxPolicy;

@Repository
class TaxPolicyLoader {
    private final JdbcClient jdbcClient;

    TaxPolicyLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    TaxPolicy load() {
        return jdbcClient.sql("""
                        select policy_id, name, tax_rate, service_fee_rate, tip_editable
                        from tax_policies
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """)
                .query((rs, rowNum) -> new TaxPolicy(
                        rs.getObject("policy_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("tax_rate"),
                        rs.getBigDecimal("service_fee_rate"),
                        rs.getBoolean("tip_editable")
                ))
                .single();
    }
}
