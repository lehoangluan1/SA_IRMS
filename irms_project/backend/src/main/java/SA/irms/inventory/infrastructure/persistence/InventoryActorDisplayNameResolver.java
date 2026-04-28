package SA.irms.inventory.infrastructure.persistence;

import SA.irms.common.identity.SharedIdentityDirectoryPort;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class InventoryActorDisplayNameResolver {
    private final SharedIdentityDirectoryPort identityDirectoryPort;

    InventoryActorDisplayNameResolver(SharedIdentityDirectoryPort identityDirectoryPort) {
        this.identityDirectoryPort = identityDirectoryPort;
    }

    Map<UUID, String> resolveDisplayNames(Collection<UUID> userIds) {
        return identityDirectoryPort.findDisplayNames(userIds);
    }

    String resolveActor(UUID userId, String sourceType, Map<UUID, String> names) {
        if (userId == null) {
            return fallbackActor(sourceType);
        }
        return names.getOrDefault(userId, userId.toString());
    }

    String resolveDisplayName(UUID userId, Map<UUID, String> names) {
        if (userId == null) {
            return null;
        }
        return names.getOrDefault(userId, userId.toString());
    }

    private String fallbackActor(String sourceType) {
        return sourceType == null || sourceType.isBlank() ? "System" : sourceType;
    }
}
