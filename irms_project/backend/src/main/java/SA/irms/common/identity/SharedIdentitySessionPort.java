package SA.irms.common.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SharedIdentitySessionPort {
    Optional<SessionPrincipal> findActiveSessionByTokenHash(String tokenHash);

    void touchSession(UUID sessionId, Instant lastActivityAt);
}
