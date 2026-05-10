package SA.irms.common.api;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.HealthStatusResponse;
import SA.irms.common.web.RequestContext;
import SA.irms.common.application.HealthStatusService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/health")
@Profile("!api-gateway")
public class HealthController {
    private final HealthStatusService healthStatusService;
    private final String applicationName;

    public HealthController(HealthStatusService healthStatusService, @Value("${spring.application.name:irms-service}") String applicationName) {
        this.healthStatusService = healthStatusService;
        this.applicationName = applicationName;
    }

    @GetMapping
    public ApiEnvelope<HealthStatusResponse> health(HttpServletRequest request) {
        boolean databaseUp = healthStatusService.isDatabaseUp();
        return ApiEnvelope.of(
                new HealthStatusResponse(applicationName, "service", "UP", databaseUp ? "UP" : "DOWN", Instant.now()),
                RequestContext.getCorrelationId(request)
        );
    }
}
