package SA.irms.common.identity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionPrincipal(
        UUID sessionId,
        UUID userId,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> permissions,
        Instant expiresAt,
        Instant lastActivityAt
) {
}
