package SA.irms.inventory.domain;

public interface ReorderRecommendationStrategy {
    ReorderRecommendation compute(StockLevel stockLevel);
}
