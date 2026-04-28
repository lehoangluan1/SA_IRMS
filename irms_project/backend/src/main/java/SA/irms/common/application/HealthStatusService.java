package SA.irms.common.application;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import SA.irms.common.application.port.out.HealthCheckPort;

@Service
@Profile("!api-gateway")
public class HealthStatusService {
    private final HealthCheckPort repository;

    public HealthStatusService(HealthCheckPort repository) {
        this.repository = repository;
    }

    public boolean isDatabaseUp() {
        return repository.databaseCheck() == 1;
    }
}
