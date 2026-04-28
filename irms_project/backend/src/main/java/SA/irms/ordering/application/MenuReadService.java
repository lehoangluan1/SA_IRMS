package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;

@Service
class MenuReadService {
    private final OrderQueryRepository queryRepository;

    MenuReadService(OrderQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    SA.irms.ordering.application.view.MenuViews.MenuOverview load() {
        return new SA.irms.ordering.application.view.MenuViews.MenuOverview(loadCategories(), loadItems(), loadPromotions(), loadCombos());
    }

    SA.irms.ordering.application.view.MenuViews.CategoryView findCategory(UUID categoryId) {
        return loadCategories().stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Category was not found."));
    }

    SA.irms.ordering.application.view.MenuViews.MenuItemView findItem(UUID menuItemId) {
        return loadItems().stream()
                .filter(item -> item.id().equals(menuItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Menu item was not found."));
    }

    SA.irms.ordering.application.view.MenuViews.PromotionView findPromotion(UUID promotionId) {
        return loadPromotions().stream()
                .filter(promotion -> promotion.id().equals(promotionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Promotion was not found."));
    }

    SA.irms.ordering.application.view.MenuViews.ComboView findCombo(UUID comboId) {
        return queryRepository.loadMenuCombo(comboId);
    }

    List<SA.irms.ordering.application.view.MenuViews.CategoryView> loadCategories() {
        return queryRepository.loadCategories();
    }

    List<SA.irms.ordering.application.view.MenuViews.MenuItemView> loadItems() {
        return queryRepository.loadMenuItems();
    }

    List<SA.irms.ordering.application.view.MenuViews.PromotionView> loadPromotions() {
        return queryRepository.loadPromotions();
    }

    List<SA.irms.ordering.application.view.MenuViews.ComboView> loadCombos() {
        return queryRepository.loadMenuCombos();
    }
}
