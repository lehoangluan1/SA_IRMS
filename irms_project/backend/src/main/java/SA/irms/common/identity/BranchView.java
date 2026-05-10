package SA.irms.common.identity;

import java.util.UUID;

public record BranchView(UUID branchId, String code, String name, String timezone) {
}
