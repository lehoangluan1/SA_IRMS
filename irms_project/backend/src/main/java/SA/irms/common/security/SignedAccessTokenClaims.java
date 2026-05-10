package SA.irms.common.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SignedAccessTokenClaims(
        UUID sessionId,
        UUID userId,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt
) {
}
