package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.ordering.application.command.OrderCommands.ComboGroupSelectionRequest;
import SA.irms.ordering.application.command.OrderCommands.ComboSelectionRequest;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.domain.ComboPricingPolicy;

@Component
class OrderComboSelectionResolver {
    private final OrderQueryRepository queryRepository;
    private final ComboPricingPolicy comboPricingPolicy;
    private final OrderCreationValidator validator;

    OrderComboSelectionResolver(
            OrderQueryRepository queryRepository,
            ComboPricingPolicy comboPricingPolicy,
            OrderCreationValidator validator
    ) {
        this.queryRepository = queryRepository;
        this.comboPricingPolicy = comboPricingPolicy;
        this.validator = validator;
    }

    ResolvedComboSelection resolve(ComboSelectionRequest comboSelection) {
        validator.validateComboQuantity(comboSelection);
        validator.validateComboGroupsPresent(comboSelection);

        OrderQueryRepository.ResolvedComboRow combo = queryRepository.loadResolvedCombo(comboSelection.comboId());
        validator.ensureComboIsActive(combo);

        int totalComponentUnits = comboSelection.groups().stream()
                .mapToInt(group -> group.selectedOptionIds() == null ? 0 : group.selectedOptionIds().size() * comboSelection.quantity())
                .sum();

        List<Map<String, Object>> groupPayloads = new ArrayList<>();
        List<ResolvedComboComponent> components = new ArrayList<>();
        for (OrderQueryRepository.ResolvedComboGroupRow group : combo.groups()) {
            ComboGroupSelectionRequest requestedGroup = comboSelection.groups().stream()
                    .filter(candidate -> candidate.comboGroupId().equals(group.comboGroupId()))
                    .findFirst()
                    .orElse(null);
            List<UUID> selectedOptionIds = requestedGroup == null || requestedGroup.selectedOptionIds() == null
                    ? List.of()
                    : requestedGroup.selectedOptionIds();
            validator.validateGroupSelection(group, selectedOptionIds);
            List<OrderQueryRepository.ResolvedComboOptionRow> options = queryRepository.loadResolvedComboOptions(
                    combo.comboId(),
                    group.comboGroupId(),
                    selectedOptionIds
            );
            validator.ensureResolvedComboOptions(group, selectedOptionIds, options);
            groupPayloads.add(Map.of(
                    "comboGroupId", group.comboGroupId().toString(),
                    "selectedOptionIds", selectedOptionIds.stream().map(UUID::toString).toList()
            ));
            for (OrderQueryRepository.ResolvedComboOptionRow option : options) {
                BigDecimal allocatedUnitPrice = comboPricingPolicy.componentUnitPrice(
                        combo.comboPrice(),
                        option.extraPrice(),
                        totalComponentUnits
                );
                components.add(new ResolvedComboComponent(
                        group.comboGroupId(),
                        option.comboOptionId(),
                        option.menuItemId(),
                        option.menuItemName(),
                        option.station(),
                        comboSelection.quantity(),
                        allocatedUnitPrice
                ));
            }
        }
        return new ResolvedComboSelection(
                combo.comboId(),
                combo.name(),
                combo.comboPrice(),
                comboSelection.quantity(),
                comboSelection.allergyNotes(),
                comboSelection.specialInstructions(),
                groupPayloads,
                components
        );
    }

    record ResolvedComboSelection(
            UUID comboId,
            String comboName,
            BigDecimal comboPrice,
            int quantity,
            String allergyNotes,
            String specialInstructions,
            List<Map<String, Object>> groupPayloads,
            List<ResolvedComboComponent> components
    ) {
        BigDecimal subtotal() {
            return comboPrice.multiply(BigDecimal.valueOf(quantity));
        }

        Map<String, Object> payload() {
            return Map.of(
                    "comboId", comboId.toString(),
                    "comboName", comboName,
                    "quantity", quantity,
                    "allergyNotes", allergyNotes == null ? "" : allergyNotes,
                    "specialInstructions", specialInstructions == null ? "" : specialInstructions,
                    "groups", groupPayloads
            );
        }
    }

    record ResolvedComboComponent(
            UUID comboGroupId,
            UUID comboOptionId,
            UUID menuItemId,
            String menuItemName,
            String station,
            int quantity,
            BigDecimal allocatedUnitPrice
    ) {
    }
}
