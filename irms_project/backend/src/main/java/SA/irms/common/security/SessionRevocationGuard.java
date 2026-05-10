package SA.irms.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import SA.irms.common.config.AppProperties;
import SA.irms.common.identity.SharedIdentitySessionPort;

@Component
public class SessionRevocationGuard {
    private final SharedIdentitySessionPort identitySessionPort;
    private final TokenHashingService tokenHashingService;
    private final AppProperties properties;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SessionRevocationGuard(
            SharedIdentitySessionPort identitySessionPort,
            TokenHashingService tokenHashingService,
            AppProperties properties,
            Clock clock
    ) {
        this.identitySessionPort = identitySessionPort;
        this.tokenHashingService = tokenHashingService;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isSessionStillActive(String token, UUID sessionId) {
        int introspectionSeconds = properties.security().accessTokenIntrospectionSeconds();
        if (introspectionSeconds <= 0) {
            return true;
        }
        String tokenHash = tokenHashingService.hash(token);
        Instant now = Instant.now(clock);
        CacheEntry cached = cache.get(tokenHash);
        if (cached != null && cached.checkedAt().plus(Duration.ofSeconds(introspectionSeconds)).isAfter(now)) {
            return cached.active();
        }
        boolean active = identitySessionPort.findActiveSessionByTokenHash(tokenHash)
                .filter(session -> session.sessionId().equals(sessionId))
                .filter(session -> session.expiresAt().isAfter(now))
                .isPresent();
        cache.put(tokenHash, new CacheEntry(active, now));
        return active;
    }

    private record CacheEntry(boolean active, Instant checkedAt) {
    }
}
