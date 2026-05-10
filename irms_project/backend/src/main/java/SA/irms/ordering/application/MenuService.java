package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import SA.irms.common.context.RequestMetadata;

@Service
public class MenuService {
    private final MenuReadService menuReadService;
    private final MenuCategoryService menuCategoryService;
    private final MenuItemService menuItemService;
    private final MenuPromotionService menuPromotionService;
    private final MenuComboService menuComboService;

    public MenuService(MenuReadService menuReadService, MenuCategoryService menuCategoryService,
            MenuItemService menuItemService, MenuPromotionService menuPromotionService, MenuComboService menuComboService) {
        this.menuReadService = menuReadService;
        this.menuCategoryService = menuCategoryService;
        this.menuItemService = menuItemService;
        this.menuPromotionService = menuPromotionService;
        this.menuComboService = menuComboService;
    }

    public SA.irms.ordering.application.view.MenuViews.MenuOverview load() { return menuReadService.load(); }
    public java.util.List<SA.irms.ordering.application.view.MenuViews.CategoryView> loadCategories() { return menuReadService.loadCategories(); }
    public SA.irms.ordering.application.view.MenuViews.CategoryView findCategory(UUID categoryId) { return menuReadService.findCategory(categoryId); }
    public SA.irms.ordering.application.view.MenuViews.CategoryView createCategory(String name) { return menuCategoryService.createCategory(name); }
    public SA.irms.ordering.application.view.MenuViews.CategoryView updateCategory(UUID categoryId, String name) { return menuCategoryService.updateCategory(categoryId, name); }
    public void deleteCategory(UUID categoryId) { menuCategoryService.deleteCategory(categoryId); }
    public java.util.List<SA.irms.ordering.application.view.MenuViews.MenuItemView> loadItems() { return menuReadService.loadItems(); }
    public SA.irms.ordering.application.view.MenuViews.MenuItemView findItem(UUID menuItemId) { return menuReadService.findItem(menuItemId); }
    public SA.irms.ordering.application.view.MenuViews.MenuItemView createItem(SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        return menuItemService.createItem(request, actorUserId, correlationId, httpServletRequest);
    }
    public SA.irms.ordering.application.view.MenuViews.MenuItemView updateItem(UUID menuItemId, SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        return menuItemService.updateItem(menuItemId, request, actorUserId, correlationId, httpServletRequest);
    }
    public SA.irms.ordering.application.view.MenuViews.MenuItemView toggleAvailability(UUID menuItemId, UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        return menuItemService.toggleAvailability(menuItemId, actorUserId, correlationId, httpServletRequest);
    }
    public void deleteItem(UUID menuItemId) { menuItemService.deleteItem(menuItemId); }
    public java.util.List<SA.irms.ordering.application.view.MenuViews.PromotionView> loadPromotions() { return menuReadService.loadPromotions(); }
    public SA.irms.ordering.application.view.MenuViews.PromotionView findPromotion(UUID promotionId) { return menuReadService.findPromotion(promotionId); }
    public SA.irms.ordering.application.view.MenuViews.PromotionView createPromotion(SA.irms.ordering.application.command.MenuCommands.PromotionUpsert request) { return menuPromotionService.createPromotion(request); }
    public java.util.List<SA.irms.ordering.application.view.MenuViews.ComboView> loadCombos() { return menuReadService.loadCombos(); }
    public SA.irms.ordering.application.view.MenuViews.ComboView findCombo(UUID comboId) { return menuReadService.findCombo(comboId); }
    public SA.irms.ordering.application.view.MenuViews.ComboView createCombo(SA.irms.ordering.application.command.MenuCommands.ComboUpsert request) { return menuComboService.createCombo(request); }
    public SA.irms.ordering.application.view.MenuViews.ComboView updateCombo(UUID comboId, SA.irms.ordering.application.command.MenuCommands.ComboUpsert request) { return menuComboService.updateCombo(comboId, request); }
    public SA.irms.ordering.application.view.MenuViews.PromotionView updatePromotion(UUID promotionId, SA.irms.ordering.application.command.MenuCommands.PromotionUpsert request) { return menuPromotionService.updatePromotion(promotionId, request); }
    public void deletePromotion(UUID promotionId) { menuPromotionService.deletePromotion(promotionId); }
    public BigDecimal applyPromotion(UUID billId, String code) { return menuPromotionService.applyPromotion(billId, code); }

}
