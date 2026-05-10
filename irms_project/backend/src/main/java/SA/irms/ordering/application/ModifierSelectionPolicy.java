package SA.irms.ordering.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;

@Component
class ModifierSelectionPolicy {
    void validate(
            List<OrderMenuSelectionService.ModifierGroupConfig> groups,
            List<OrderMenuSelectionService.ResolvedModifierOption> selectedOptions
    ) {
        Map<UUID, Long> selectedCountByGroup = selectedOptions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        OrderMenuSelectionService.ResolvedModifierOption::groupId,
                        java.util.stream.Collectors.counting()
                ));
        for (OrderMenuSelectionService.ModifierGroupConfig group : groups) {
            long count = selectedCountByGroup.getOrDefault(group.id(), 0L);
            if ((group.required() || group.minSelect() > 0) && count < group.minSelect()) {
                throw new ConflictException("Modifier group '" + group.name() + "' requires at least " + group.minSelect() + " selection(s).");
            }
            if (count > group.maxSelect()) {
                throw new ConflictException("Modifier group '" + group.name() + "' allows at most " + group.maxSelect() + " selection(s).");
            }
            if (!group.multiSelect() && count > 1) {
                throw new ConflictException("Modifier group '" + group.name() + "' only allows one selection.");
            }
        }
    }
}
