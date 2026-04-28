package SA.irms.inventory.infrastructure.persistence;

import SA.irms.common.error.NotFoundException;
import SA.irms.inventory.application.port.out.InventoryQueryRepository;
import SA.irms.inventory.application.view.InventoryViews.InventoryOverview;
import SA.irms.inventory.application.view.InventoryViews.IngredientView;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInventoryQueryRepository implements InventoryQueryRepository {
    private final JdbcInventoryItemQueryRepository inventoryItemQueries;
    private final JdbcStockTransactionQueryRepository stockTransactionQueries;
    private final JdbcReorderRecommendationQueryRepository reorderRecommendationQueries;
    private final JdbcLowStockAlertQueryRepository lowStockAlertQueries;

    JdbcInventoryQueryRepository(
            JdbcInventoryItemQueryRepository inventoryItemQueries,
            JdbcStockTransactionQueryRepository stockTransactionQueries,
            JdbcReorderRecommendationQueryRepository reorderRecommendationQueries,
            JdbcLowStockAlertQueryRepository lowStockAlertQueries
    ) {
        this.inventoryItemQueries = inventoryItemQueries;
        this.stockTransactionQueries = stockTransactionQueries;
        this.reorderRecommendationQueries = reorderRecommendationQueries;
        this.lowStockAlertQueries = lowStockAlertQueries;
    }

    @Override
    public InventoryOverview load() {
        return new InventoryOverview(
                inventoryItemQueries.loadItems(),
                stockTransactionQueries.loadTransactions(),
                reorderRecommendationQueries.loadRecommendations(),
                lowStockAlertQueries.loadAlerts()
        );
    }

    @Override
    public IngredientView findIngredient(UUID inventoryItemId) {
        return inventoryItemQueries.findIngredient(inventoryItemId)
                .orElseThrow(() -> new NotFoundException("Ingredient was not found."));
    }
}
