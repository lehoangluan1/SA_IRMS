package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.PreparationPolicy;

@Repository
class PreparationPolicyLoader {
    private final JdbcClient jdbcClient;

    PreparationPolicyLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    PreparationPolicy load() {
        return jdbcClient.sql("""
                        select policy_id, group_by_station, supports_batching, rush_threshold_min
                        from preparation_policies
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """)
                .query((rs, rowNum) -> new PreparationPolicy(
                        rs.getObject("policy_id", UUID.class),
                        rs.getBoolean("group_by_station"),
                        rs.getBoolean("supports_batching"),
                        rs.getInt("rush_threshold_min")
                ))
                .single();
    }
}
