package SA.irms.kitchen.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;
import SA.irms.kitchen.application.port.out.KitchenInventoryConsumptionPort;

@Component
@Profile("kitchen-service")
class RemoteInventoryConsumptionClient implements KitchenInventoryConsumptionPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    RemoteInventoryConsumptionClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public void consumeForKitchenStart(UUID orderItemId, UUID actorUserId, String correlationId) {
        restTemplate.postForLocation(
                endpoints.inventoryBaseUrl() + "/internal/inventory/consumption/kitchen-start",
                new KitchenStartConsumptionRequest(orderItemId, actorUserId, correlationId)
        );
    }

    record KitchenStartConsumptionRequest(UUID orderItemId, UUID actorUserId, String correlationId) {
    }
}
