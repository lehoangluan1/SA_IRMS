package SA.irms.common.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SharedIdentityDirectoryPort {
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);

    Optional<UUID> findFirstActiveUserIdByRolePriority(List<String> rolePriority);
}
