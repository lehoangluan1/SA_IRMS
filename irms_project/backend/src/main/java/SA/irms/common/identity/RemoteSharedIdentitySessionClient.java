package SA.irms.common.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;


@Component
@Profile("!identity-audit-service")
public class RemoteSharedIdentitySessionClient implements SharedIdentitySessionPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    public RemoteSharedIdentitySessionClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public Optional<SessionPrincipal> findActiveSessionByTokenHash(String tokenHash) {
        try {
            ResponseEntity<SessionPrincipal> response = restTemplate.getForEntity(
                    endpoints.identityBaseUrl() + "/internal/identity/sessions/by-token-hash/{tokenHash}",
                    SessionPrincipal.class,
                    tokenHash
            );
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound ignored) {
            return Optional.empty();
        } catch (RestClientException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void touchSession(UUID sessionId, Instant lastActivityAt) {
        restTemplate.postForLocation(
                endpoints.identityBaseUrl() + "/internal/identity/sessions/{sessionId}/touch",
                new TouchSessionRequest(lastActivityAt),
                sessionId
        );
    }
}
