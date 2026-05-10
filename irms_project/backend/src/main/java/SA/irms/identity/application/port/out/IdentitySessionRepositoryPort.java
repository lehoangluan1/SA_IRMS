package SA.irms.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentitySessionRepositoryPort {
    Optional<SessionPrincipal> findActiveSessionByTokenHash(String tokenHash);

    void touchSession(UUID sessionId, Instant lastActivityAt);

    void terminateSession(UUID sessionId, Instant endedAt);

    UUID createSession(UUID sessionId, UUID userId, String tokenHash, Instant startedAt, Instant expiresAt, String deviceId, String ipAddress);

    record SessionPrincipal(
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
}
