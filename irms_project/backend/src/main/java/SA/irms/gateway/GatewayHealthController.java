package SA.irms.gateway;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.HealthStatusResponse;
import SA.irms.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/health")
class GatewayHealthController {
    private final String applicationName;

    GatewayHealthController(@Value("${spring.application.name:irms-api-gateway}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    ApiEnvelope<HealthStatusResponse> health(HttpServletRequest request) {
        return ApiEnvelope.of(
                new HealthStatusResponse(applicationName, "gateway", "UP", null, Instant.now()),
                RequestContext.getCorrelationId(request)
        );
    }
}
