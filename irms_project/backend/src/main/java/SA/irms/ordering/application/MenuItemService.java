package SA.irms.ordering.application;

import SA.irms.ordering.application.support.MenuJsonSupport;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.application.events.MenuItemAvailabilityChangedEvent;
import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.common.context.RequestMetadata;

@Service
class MenuItemService {
    private final OrderRepository repository;
    private final AuditRecorder auditService;
    private final MenuReadService menuReadService;
    private final MenuRecipeService menuRecipeService;
    private final MenuJsonSupport menuJsonSupport;
    private final DomainEventPublisher outboxPublisher;

    MenuItemService(OrderRepository repository, AuditRecorder auditService, MenuReadService menuReadService,
                    MenuRecipeService menuRecipeService, MenuJsonSupport menuJsonSupport, DomainEventPublisher outboxPublisher) {
        this.repository = repository;
        this.auditService = auditService;
        this.menuReadService = menuReadService;
        this.menuRecipeService = menuRecipeService;
        this.menuJsonSupport = menuJsonSupport;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.MenuItemView createItem(SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        UUID categoryId = findCategoryIdByName(request.category());
        UUID menuItemId = UUID.randomUUID();
        repository.createMenuItem(menuItemId, categoryId, request, menuJsonSupport.toJsonArray(request.allergens()));
        menuRecipeService.writeRecipe(menuItemId, request.ingredients(), 1);
        auditService.record(actorUserId, "menu.item.created", "MenuItem", menuItemId.toString(), correlationId, null, false,
                httpServletRequest.remoteIp(), Map.of(), Map.of("name", request.name(), "price", request.price(), "station", request.station()));
        return menuReadService.findItem(menuItemId);
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.MenuItemView updateItem(UUID menuItemId, SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        SA.irms.ordering.application.view.MenuViews.MenuItemView existing = menuReadService.findItem(menuItemId);
        UUID categoryId = findCategoryIdByName(request.category());
        if (!repository.updateMenuItem(menuItemId, categoryId, request, menuJsonSupport.toJsonArray(request.allergens()))) {
            throw new NotFoundException("Menu item was not found.");
        }
        repository.retireActiveRecipes(menuItemId);
        menuRecipeService.writeRecipe(menuItemId, request.ingredients(), existing.version() + 1);
        outboxPublisher.publish(new MenuItemAvailabilityChangedEvent(menuItemId.toString(), Map.of(
                "menuItemId", menuItemId.toString(),
                "name", request.name(),
                "price", request.price(),
                "available", existing.available(),
                "changeType", "MENU_ITEM_UPDATED"
        )), correlationId, null);
        auditService.record(actorUserId, "menu.item.updated", "MenuItem", menuItemId.toString(), correlationId,
                "Menu and pricing updated.", false, httpServletRequest.remoteIp(),
                Map.of("name", existing.name(), "price", existing.price()), Map.of("name", request.name(), "price", request.price()));
        return menuReadService.findItem(menuItemId);
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.MenuItemView toggleAvailability(UUID menuItemId, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        SA.irms.ordering.application.view.MenuViews.MenuItemView existing = menuReadService.findItem(menuItemId);
        repository.updateMenuItemAvailability(menuItemId, existing.available() ? "unavailable" : "available", existing.available() ? "hidden" : "active");
        outboxPublisher.publish(new MenuItemAvailabilityChangedEvent(menuItemId.toString(), Map.of(
                "menuItemId", menuItemId.toString(),
                "name", existing.name(),
                "available", !existing.available(),
                "previousAvailable", existing.available(),
                "changeType", "AVAILABILITY_TOGGLED"
        )), correlationId, null);
        auditService.record(actorUserId, "menu.item.availability.changed", "MenuItem", menuItemId.toString(), correlationId, null, false,
                httpServletRequest.remoteIp(), Map.of("available", existing.available()), Map.of("available", !existing.available()));
        return menuReadService.findItem(menuItemId);
    }

    @Transactional
    void deleteItem(UUID menuItemId) {
        if (!repository.deleteMenuItem(menuItemId)) {
            throw new NotFoundException("Menu item was not found.");
        }
    }

    private UUID findCategoryIdByName(String categoryName) {
        return repository.findCategoryIdByName(categoryName)
                .orElseThrow(() -> new NotFoundException("Menu category was not found."));
    }
}
