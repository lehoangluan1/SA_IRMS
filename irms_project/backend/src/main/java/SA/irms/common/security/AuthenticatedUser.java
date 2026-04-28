package SA.irms.common.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> permissions,
        UUID sessionId
) {
    public boolean hasPermission(String permission) {
        return permissions.contains("all") || permissions.contains(permission);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
