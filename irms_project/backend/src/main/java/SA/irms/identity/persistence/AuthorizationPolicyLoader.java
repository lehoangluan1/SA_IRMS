package SA.irms.identity.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort.AuthorizationPolicy;

@Repository
class AuthorizationPolicyLoader {
    private final JdbcClient jdbcClient;

    AuthorizationPolicyLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    AuthorizationPolicy load() {
        return jdbcClient.sql("""
                        select policy_id,
                               policy_key,
                               requires_reason_for_override,
                               max_refund_limit,
                               refund_window_hours,
                               session_idle_timeout_minutes,
                               session_absolute_timeout_hours
                        from authorization_policies
                        where policy_key = 'default'
                        """)
                .query((rs, rowNum) -> new AuthorizationPolicy(
                        rs.getObject("policy_id", UUID.class),
                        rs.getString("policy_key"),
                        rs.getBoolean("requires_reason_for_override"),
                        rs.getBigDecimal("max_refund_limit"),
                        rs.getInt("refund_window_hours"),
                        rs.getInt("session_idle_timeout_minutes"),
                        rs.getInt("session_absolute_timeout_hours")
                ))
                .single();
    }
}
