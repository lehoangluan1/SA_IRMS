package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;

@Service
public class MenuComboService {
    private final OrderRepository repository;
    private final OrderQueryRepository queryRepository;

    public MenuComboService(OrderRepository repository, OrderQueryRepository queryRepository) {
        this.repository = repository;
        this.queryRepository = queryRepository;
    }

    public List<SA.irms.ordering.application.view.MenuViews.ComboView> loadCombos() {
        return queryRepository.loadMenuCombos();
    }

    @Transactional
    public SA.irms.ordering.application.view.MenuViews.ComboView createCombo(SA.irms.ordering.application.command.MenuCommands.ComboUpsert request) {
        UUID comboId = UUID.randomUUID();
        repository.createMenuCombo(comboId, request.name(), request.description(), request.price(), Boolean.TRUE.equals(request.active()));
        repository.replaceMenuComboGroups(comboId, request.groups() == null ? List.of() : request.groups());
        return queryRepository.loadMenuCombo(comboId);
    }

    @Transactional
    public SA.irms.ordering.application.view.MenuViews.ComboView updateCombo(UUID comboId, SA.irms.ordering.application.command.MenuCommands.ComboUpsert request) {
        repository.updateMenuCombo(comboId, request.name(), request.description(), request.price(), Boolean.TRUE.equals(request.active()));
        repository.replaceMenuComboGroups(comboId, request.groups() == null ? List.of() : request.groups());
        return queryRepository.loadMenuCombo(comboId);
    }
}
