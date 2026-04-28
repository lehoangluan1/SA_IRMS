package SA.irms.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "irms.gateway")
public record GatewayRoutesProperties(
        String orderingBaseUrl,
        String kitchenBaseUrl,
        String billingBaseUrl,
        String reservationBaseUrl,
        String identityBaseUrl,
        String inventoryBaseUrl,
        String notificationBaseUrl,
        String reportingBaseUrl
) {
    public GatewayRoutesProperties {
        orderingBaseUrl = defaultIfBlank(orderingBaseUrl, "http://localhost:8081");
        kitchenBaseUrl = defaultIfBlank(kitchenBaseUrl, "http://localhost:8082");
        billingBaseUrl = defaultIfBlank(billingBaseUrl, "http://localhost:8083");
        reservationBaseUrl = defaultIfBlank(reservationBaseUrl, "http://localhost:8084");
        identityBaseUrl = defaultIfBlank(identityBaseUrl, "http://localhost:8088");
        inventoryBaseUrl = defaultIfBlank(inventoryBaseUrl, "http://localhost:8085");
        notificationBaseUrl = defaultIfBlank(notificationBaseUrl, "http://localhost:8086");
        reportingBaseUrl = defaultIfBlank(reportingBaseUrl, "http://localhost:8087");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
