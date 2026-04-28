package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.error.ConflictException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;

@Service
class OrderMenuSelectionService {
    private final OrderQueryRepository queryRepository;
    private final OrderRepository repository;
    private final ModifierSelectionPolicy modifierSelectionPolicy;

    OrderMenuSelectionService(OrderQueryRepository queryRepository, OrderRepository repository, ModifierSelectionPolicy modifierSelectionPolicy) {
        this.queryRepository = queryRepository;
        this.repository = repository;
        this.modifierSelectionPolicy = modifierSelectionPolicy;
    }

    public ResolvedMenuItem requireAvailableMenuItem(UUID menuItemId, boolean draftConfirmation) {
        ResolvedMenuItem item = queryRepository.findResolvedMenuItem(menuItemId)
                .map(row -> new ResolvedMenuItem(row.id(), row.name(), row.basePrice(), row.station(), row.availability()))
                .orElseThrow(() -> new ConflictException("Menu item was not found."));
        if (!draftConfirmation && !"available".equalsIgnoreCase(item.availability())) {
            throw new ConflictException("Menu item is currently unavailable.");
        }
        return item;
    }

    public ModifierSelection resolveModifierSelection(UUID menuItemId, List<UUID> requestedOptionIds) {
        List<ModifierGroupConfig> groups = queryRepository.loadModifierGroupConfigs(menuItemId).stream()
                .map(row -> new ModifierGroupConfig(row.id(), row.name(), row.minSelect(), row.maxSelect(), row.required(), row.multiSelect()))
                .toList();
        List<ResolvedModifierOption> selectedOptions = queryRepository.loadResolvedModifierOptions(menuItemId, requestedOptionIds).stream()
                .map(row -> new ResolvedModifierOption(row.optionId(), row.groupId(), row.name(), row.extraPrice()))
                .toList();
        if (requestedOptionIds != null && requestedOptionIds.size() != selectedOptions.size()) {
            throw new ConflictException("One or more modifier selections are invalid for the selected menu item.");
        }
        modifierSelectionPolicy.validate(groups, selectedOptions);
        BigDecimal extraPrice = selectedOptions.stream()
                .map(ResolvedModifierOption::extraPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ModifierSelection(selectedOptions, extraPrice);
    }

    public void insertModifierSelections(UUID orderItemId, List<ResolvedModifierOption> selectedOptions) {
        repository.insertModifierSelections(orderItemId, selectedOptions.stream()
                .map(option -> new OrderRepository.ModifierSelectionRow(option.optionId(), option.name(), option.extraPrice()))
                .toList());
    }

    public void refreshModifierSelections(UUID orderItemId, List<ResolvedModifierOption> selectedOptions) {
        repository.replaceModifierSelections(orderItemId, selectedOptions.stream()
                .map(option -> new OrderRepository.ModifierSelectionRow(option.optionId(), option.name(), option.extraPrice()))
                .toList());
    }

    public List<UUID> loadModifierOptionIds(UUID orderItemId) {
        return queryRepository.loadModifierOptionIds(orderItemId);
    }

    public List<SA.irms.ordering.application.view.OrderViews.ModifierGroupView> loadModifierGroups(UUID menuItemId) {
        return queryRepository.loadModifierGroups(menuItemId);
    }

    record ResolvedMenuItem(UUID id, String name, BigDecimal basePrice, String station, String availability) {
    }

    record ResolvedModifierOption(UUID optionId, UUID groupId, String name, BigDecimal extraPrice) {
    }

    record ModifierSelection(List<ResolvedModifierOption> selectedOptions, BigDecimal extraPrice) {
    }

    record ModifierGroupConfig(UUID id, String name, int minSelect, int maxSelect, boolean required, boolean multiSelect) {
    }
}
