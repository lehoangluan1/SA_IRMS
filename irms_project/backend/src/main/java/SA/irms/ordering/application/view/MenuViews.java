package SA.irms.ordering.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class MenuViews {
    private MenuViews() {}

    public record MenuOverview(List<CategoryView> categories, List<MenuItemView> items, List<PromotionView> promotions, List<ComboView> combos) {}
    public record CategoryView(UUID id, String name, int items, boolean active) {}
    public record MenuItemView(UUID id, String name, String description, String category, BigDecimal price,
                               String station, boolean available, List<String> allergens, int modifiers,
                               int version, List<String> ingredients) {}
    public record PromotionView(UUID id, String code, String discount, String validUntil, boolean active) {}
    public record ComboView(UUID id, String name, String description, BigDecimal price, boolean active,
                            List<ComboGroupView> groups) {}
    public record ComboGroupView(UUID id, String name, int minSelections, int maxSelections, boolean required,
                                 List<ComboOptionView> options) {}
    public record ComboOptionView(UUID id, UUID menuItemId, String menuItemName, String station, BigDecimal extraPrice, boolean active) {}
}
