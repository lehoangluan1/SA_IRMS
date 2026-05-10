package SA.irms.ordering.domain;

import java.util.List;
import java.util.UUID;

public record ComboGroup(
        UUID groupId,
        String name,
        int minSelections,
        int maxSelections,
        boolean required,
        List<ComboItemOption> options
) {
}
