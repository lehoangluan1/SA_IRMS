package SA.irms.common.identity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;
import SA.irms.common.identity.DisplayNameLookupRequest;
import SA.irms.common.identity.DisplayNameLookupResponse;
import SA.irms.common.identity.RolePriorityUserRequest;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.UserIdResponse;

@Component
@Profile("!identity-audit-service")
public class RemoteSharedIdentityDirectoryClient implements SharedIdentityDirectoryPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    public RemoteSharedIdentityDirectoryClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        List<UUID> distinctIds = userIds == null ? List.of() : new LinkedHashSet<>(userIds).stream().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        DisplayNameLookupResponse response = restTemplate.postForObject(
                endpoints.identityBaseUrl() + "/internal/identity/users/display-names",
                new DisplayNameLookupRequest(distinctIds),
                DisplayNameLookupResponse.class
        );
        if (response == null || response.users() == null) {
            return Map.of();
        }
        return response.users().stream().collect(Collectors.toUnmodifiableMap(
                user -> user.userId(),
                user -> user.displayName(),
                (left, right) -> left
        ));
    }

    @Override
    public Optional<UUID> findFirstActiveUserIdByRolePriority(List<String> rolePriority) {
        try {
            UserIdResponse response = restTemplate.postForObject(
                    endpoints.identityBaseUrl() + "/internal/identity/users/first-active-by-role-priority",
                    new RolePriorityUserRequest(rolePriority == null ? List.of() : rolePriority),
                    UserIdResponse.class
            );
            return response == null ? Optional.empty() : Optional.ofNullable(response.userId());
        } catch (HttpClientErrorException.NotFound ignored) {
            return Optional.empty();
        }
    }
}
