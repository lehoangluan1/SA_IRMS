package SA.irms.identity.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IdentityDirectoryRepositoryPort {
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);

    Optional<UUID> findFirstActiveUserIdByRolePriority(List<String> rolePriority);
}
