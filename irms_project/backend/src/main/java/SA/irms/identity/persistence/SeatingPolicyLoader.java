package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.SeatingPolicy;

@Repository
class SeatingPolicyLoader {
    private final JdbcClient jdbcClient;

    SeatingPolicyLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    SeatingPolicy load() {
        return jdbcClient.sql("""
                        select policy_id, max_hold_minutes, walk_in_bias, reservation_grace_minutes
                        from seating_policies
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """)
                .query((rs, rowNum) -> new SeatingPolicy(
                        rs.getObject("policy_id", UUID.class),
                        rs.getInt("max_hold_minutes"),
                        rs.getBigDecimal("walk_in_bias"),
                        rs.getInt("reservation_grace_minutes")
                ))
                .single();
    }
}
