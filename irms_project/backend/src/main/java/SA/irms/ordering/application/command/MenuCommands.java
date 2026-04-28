package SA.irms.ordering.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class MenuCommands {
    private MenuCommands() {}

    public record MenuItemUpsert(String name, String description, String category, BigDecimal price, String station,
                                 List<String> allergens, List<String> ingredients, Integer preparationTimeMin) {}
    public record PromotionUpsert(String code, String discount, String validUntil) {}
    public record ComboUpsert(String name, String description, BigDecimal price, Boolean active,
                              List<ComboGroupUpsert> groups) {}
    public record ComboGroupUpsert(String name, Integer minSelections, Integer maxSelections, Boolean required,
                                   List<ComboOptionUpsert> options) {}
    public record ComboOptionUpsert(UUID menuItemId, BigDecimal extraPrice, Boolean active) {}
}
