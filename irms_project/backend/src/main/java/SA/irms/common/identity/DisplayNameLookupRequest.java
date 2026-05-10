package SA.irms.common.identity;

import java.util.List;
import java.util.UUID;

public record DisplayNameLookupRequest(List<UUID> userIds) {
}
