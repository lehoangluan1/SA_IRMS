package SA.irms.ordering.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;
import SA.irms.ordering.application.KitchenCoordinationPort;

@Component
@Profile("ordering-service")
class RemoteKitchenOrderRoutingClient implements KitchenCoordinationPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    RemoteKitchenOrderRoutingClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public void queueConfirmedItems(UUID orderId, List<KitchenOrderItemCommand> items, String notes) {
        restTemplate.postForLocation(
                endpoints.kitchenBaseUrl() + "/internal/kitchen/orders/" + orderId + "/tickets",
                new QueueItemsRequest(items, notes)
        );
    }

    @Override
    public void holdOrderItemForService(UUID orderItemId) {
        restTemplate.postForLocation(endpoints.kitchenBaseUrl() + "/internal/kitchen/order-items/" + orderItemId + "/hold", null);
    }

    @Override
    public void releaseHeldOrderItem(UUID orderId, KitchenOrderItemCommand item, String notes) {
        restTemplate.postForLocation(
                endpoints.kitchenBaseUrl() + "/internal/kitchen/orders/" + orderId + "/items/release",
                new ReleaseItemRequest(item, notes)
        );
    }

    @Override
    public void blockOrderItem(UUID orderItemId, String reason) {
        restTemplate.postForLocation(
                endpoints.kitchenBaseUrl() + "/internal/kitchen/order-items/" + orderItemId + "/block",
                new BlockItemRequest(reason)
        );
    }

    record QueueItemsRequest(List<KitchenOrderItemCommand> items, String notes) {
    }

    record ReleaseItemRequest(KitchenOrderItemCommand item, String notes) {
    }

    record BlockItemRequest(String reason) {
    }
}
