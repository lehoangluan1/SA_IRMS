package SA.irms.billing.integration;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import SA.irms.billing.application.port.out.PromotionGatewayPort;
import SA.irms.common.remote.ServiceEndpointProperties;

@Component
@Profile("billing-service")
class RemotePromotionApplicationClient implements PromotionGatewayPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    RemotePromotionApplicationClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public BigDecimal applyPromotion(UUID billId, String code) {
        PromotionResponse response = restTemplate.postForObject(
                endpoints.orderingBaseUrl() + "/internal/ordering/promotions/apply",
                new PromotionRequest(billId, code),
                PromotionResponse.class
        );
        return response == null ? BigDecimal.ZERO : response.discount();
    }

    record PromotionRequest(UUID billId, String code) {
    }

    record PromotionResponse(BigDecimal discount) {
    }
}
