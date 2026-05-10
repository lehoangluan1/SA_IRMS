package SA.irms.common.identity;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.remote.ServiceEndpointProperties;
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Component
@Profile("!identity-audit-service")
public class RemoteSharedIdentityPolicyClient implements SharedIdentityPolicyPort {
    private final RestTemplate restTemplate;
    private final ServiceEndpointProperties endpoints;

    public RemoteSharedIdentityPolicyClient(
            @Qualifier("serviceRestTemplate") RestTemplate restTemplate,
            ServiceEndpointProperties endpoints
    ) {
        this.restTemplate = restTemplate;
        this.endpoints = endpoints;
    }

    @Override
    public PolicySnapshot getPolicySnapshot() {
        return restTemplate.getForObject(
                endpoints.identityBaseUrl() + "/internal/identity/policy-snapshot",
                PolicySnapshot.class
        );
    }

    @Override
    public BranchView findDefaultBranch() {
        return restTemplate.getForObject(
                endpoints.identityBaseUrl() + "/internal/identity/default-branch",
                BranchView.class
        );
    }
}
