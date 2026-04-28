package SA.irms.gateway;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import SA.irms.common.error.DomainException;

@Component
class GatewayRouteLocator {
    private final GatewayRoutesProperties routes;
    private final List<GatewayRoute> routeTable;

    GatewayRouteLocator(GatewayRoutesProperties routes) {
        this.routes = routes;
        this.routeTable = List.of(
                new GatewayRoute(request -> request.startsWithAny("/api/orders", "/api/menu"), GatewayRoutesProperties::orderingBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/kitchen"), GatewayRoutesProperties::kitchenBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/billing", "/api/bills", "/api/payments", "/api/refunds"), GatewayRoutesProperties::billingBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/reservations", "/api/tables", "/api/waitlist"), GatewayRoutesProperties::reservationBaseUrl),
                new GatewayRoute(request -> request.equalsPath("/api/notifications") && request.methodIs("POST"), GatewayRoutesProperties::reservationBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/notifications"), GatewayRoutesProperties::notificationBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/inventory"), GatewayRoutesProperties::inventoryBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/audit", "/api/auth", "/api/settings", "/api/staff", "/api/shifts"), GatewayRoutesProperties::identityBaseUrl),
                new GatewayRoute(request -> request.startsWithAny("/api/dashboard", "/api/reports"), GatewayRoutesProperties::reportingBaseUrl)
        );
    }

    String targetBaseUrl(String path, String method) {
        return routeTable.stream()
                .filter(route -> route.matches(path, method))
                .findFirst()
                .map(route -> route.target(routes))
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "not_found",
                        "No API endpoint exists for " + method + " " + path + "."
                ));
    }
}
