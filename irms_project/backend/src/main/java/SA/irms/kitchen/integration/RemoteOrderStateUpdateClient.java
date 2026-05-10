package SA.irms.kitchen.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;
import SA.irms.kitchen.application.port.out.OrderingStatusSyncPort;
import SA.irms.kitchen.application.port.out.OrderItemLineStatusUpdate;

@Component
@Profile("kitchen-service")
class RemoteOrderStateUpdateClient implements OrderingStatusSyncPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    RemoteOrderStateUpdateClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public void refreshOrder(UUID orderId) {
        restTemplate.postForLocation(endpoints.orderingBaseUrl() + "/internal/ordering/orders/" + orderId + "/refresh-status", null);
    }

    @Override
    public void applyKitchenLineStatuses(UUID orderId, List<OrderItemLineStatusUpdate> lineStatuses) {
        restTemplate.postForLocation(
                endpoints.orderingBaseUrl() + "/internal/ordering/orders/" + orderId + "/line-statuses",
                new LineStatusRequest(lineStatuses)
        );
    }

    record LineStatusRequest(List<OrderItemLineStatusUpdate> items) {
    }
}
