package SA.irms.identity.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.DisplayNameLookupRequest;
import SA.irms.common.identity.DisplayNameLookupResponse;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.RolePriorityUserRequest;
import SA.irms.common.identity.SessionPrincipal;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.identity.SharedIdentitySessionPort;
import SA.irms.common.identity.TouchSessionRequest;
import SA.irms.common.identity.UserDisplayName;
import SA.irms.common.identity.UserIdResponse;

@RestController
@RequestMapping("/internal/identity")
class IdentityInternalController {
    private final SharedIdentitySessionPort sessionPort;
    private final SharedIdentityPolicyPort policyPort;
    private final SharedIdentityDirectoryPort directoryPort;

    IdentityInternalController(
            SharedIdentitySessionPort sessionPort,
            SharedIdentityPolicyPort policyPort,
            SharedIdentityDirectoryPort directoryPort
    ) {
        this.sessionPort = sessionPort;
        this.policyPort = policyPort;
        this.directoryPort = directoryPort;
    }

    @GetMapping("/sessions/by-token-hash/{tokenHash}")
    ResponseEntity<SessionPrincipal> findActiveSessionByTokenHash(@PathVariable String tokenHash) {
        return sessionPort.findActiveSessionByTokenHash(tokenHash)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{sessionId}/touch")
    ResponseEntity<Void> touchSession(@PathVariable UUID sessionId, @RequestBody TouchSessionRequest request) {
        sessionPort.touchSession(sessionId, request.lastActivityAt());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/policy-snapshot")
    PolicySnapshot getPolicySnapshot() {
        return policyPort.getPolicySnapshot();
    }

    @GetMapping("/default-branch")
    BranchView findDefaultBranch() {
        return policyPort.findDefaultBranch();
    }

    @PostMapping("/users/display-names")
    DisplayNameLookupResponse findDisplayNames(@RequestBody DisplayNameLookupRequest request) {
        Map<UUID, String> names = directoryPort.findDisplayNames(request.userIds() == null ? List.of() : request.userIds());
        return new DisplayNameLookupResponse(names.entrySet().stream()
                .map(entry -> new UserDisplayName(entry.getKey(), entry.getValue()))
                .toList());
    }

    @PostMapping("/users/first-active-by-role-priority")
    ResponseEntity<UserIdResponse> findFirstActiveUserIdByRolePriority(@RequestBody RolePriorityUserRequest request) {
        return directoryPort.findFirstActiveUserIdByRolePriority(request.roles() == null ? List.of() : request.roles())
                .map(userId -> ResponseEntity.ok(new UserIdResponse(userId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
