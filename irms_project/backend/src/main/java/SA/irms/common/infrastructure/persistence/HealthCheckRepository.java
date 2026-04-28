package SA.irms.common.infrastructure.persistence;

import SA.irms.common.application.port.out.HealthCheckPort;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!api-gateway")
public class HealthCheckRepository implements HealthCheckPort {
    private final JdbcClient jdbcClient;

    public HealthCheckRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int databaseCheck() {
        return jdbcClient.sql("select 1").query(Integer.class).single();
    }
}
