package SA.irms.ordering.domain;

import java.util.List;
import java.util.UUID;

public record ComboSelection(
        UUID comboId,
        int quantity,
        String allergyNotes,
        String specialInstructions,
        List<ComboSelectionGroup> groups
) {
    public record ComboSelectionGroup(UUID comboGroupId, List<UUID> selectedOptionIds) {
    }
}
