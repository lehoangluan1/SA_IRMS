package SA.irms.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "irms")
public record AppProperties(
        Cors cors,
        Security security,
        Restaurant restaurant,
        Reporting reporting,
        Notifications notifications,
        Receipts receipts,
        KitchenAutomation kitchenAutomation
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(
            int sessionIdleTimeoutMinutes,
            int sessionAbsoluteTimeoutHours,
            int accessTokenLifetimeMinutes,
            int sessionTouchDebounceSeconds,
            int accessTokenIntrospectionSeconds,
            String passwordDemoDefault,
            String internalServiceToken,
            String accessTokenSigningSecret
    ) {
    }

    public record Restaurant(
            String name,
            String timezone,
            String currency
    ) {
    }

    public record Reporting(int projectionRefreshSeconds) {
    }

    public record Notifications(int lowStockSlaSeconds) {
    }

    public record Receipts(String mailFrom) {
    }

    public record KitchenAutomation(
            int pollSeconds,
            int cookingToReadySeconds,
            int readyToServedSeconds
    ) {
    }
}
