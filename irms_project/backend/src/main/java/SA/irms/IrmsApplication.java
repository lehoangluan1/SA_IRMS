package SA.irms;

import java.util.Arrays;
import java.util.Locale;
import SA.irms.gateway.ApiGatewayApplication;
import SA.irms.runtime.billing.BillingServiceApplication;
import SA.irms.runtime.identity.IdentityAuditServiceApplication;
import SA.irms.runtime.inventory.InventoryServiceApplication;
import SA.irms.runtime.kitchen.KitchenServiceApplication;
import SA.irms.runtime.migration.MigrationApplication;
import SA.irms.runtime.notification.NotificationServiceApplication;
import SA.irms.runtime.ordering.OrderingServiceApplication;
import SA.irms.runtime.reporting.ReportingServiceApplication;
import SA.irms.runtime.reservation.ReservationServiceApplication;

/**
 * Compatibility launcher only.
 *
 * <p>The previous single Spring Boot monolith bootstrap is intentionally not the default runtime.
 * Start one coarse-grained service explicitly by setting IRMS_RUNTIME_MODE or IRMS_RUNTIME_SERVICE.
 */
public final class IrmsApplication {
    private IrmsApplication() {
    }

    public static void main(String[] args) {
        String service = firstNonBlank(
                System.getenv("IRMS_RUNTIME_MODE"),
                System.getenv("IRMS_RUNTIME_SERVICE"),
                System.getProperty("irms.runtime.mode"),
                System.getProperty("irms.runtime.service")
        ).trim().toLowerCase(Locale.ROOT);

        switch (service) {
            case "gateway", "api-gateway", "gateway-service" -> ApiGatewayApplication.main(args);
            case "ordering", "ordering-service" -> OrderingServiceApplication.main(args);
            case "kitchen", "kitchen-service" -> KitchenServiceApplication.main(args);
            case "billing", "billing-service" -> BillingServiceApplication.main(args);
            case "reservation", "reservation-service" -> ReservationServiceApplication.main(args);
            case "identity", "identity-audit", "identity-audit-service" -> IdentityAuditServiceApplication.main(args);
            case "inventory", "inventory-service" -> InventoryServiceApplication.main(args);
            case "notification", "notification-service" -> NotificationServiceApplication.main(args);
            case "reporting", "reporting-service" -> ReportingServiceApplication.main(args);
            case "migration" -> MigrationApplication.main(args);
            default -> throw new IllegalStateException(
                    "The monolithic backend runtime is disabled. Set IRMS_RUNTIME_MODE or IRMS_RUNTIME_SERVICE to one of: "
                            + "migration, api-gateway, ordering-service, kitchen-service, billing-service, reservation-service, "
                            + "identity-audit-service, inventory-service, notification-service, reporting-service."
            );
        }
    }

    private static String firstNonBlank(String... candidates) {
        return Arrays.stream(candidates)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }
}
