package SA.irms.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityUserRepository {
    Optional<UserAccount> findUserAccountByEmail(String email);

    Optional<UserAccount> findUserAccountById(UUID userId);

    void updateLastLogin(UUID userId, Instant lastLoginAt);

    void terminateActiveSessionsForUser(UUID userId, Instant endedAt);

    record UserAccount(
            UUID userId,
            String username,
            String email,
            String displayName,
            String passwordHash,
            String status,
            Set<String> roles,
            Set<String> permissions
    ) {
    }
}
