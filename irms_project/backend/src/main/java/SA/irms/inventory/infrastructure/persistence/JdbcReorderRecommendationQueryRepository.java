package SA.irms.inventory.infrastructure.persistence;

import SA.irms.inventory.application.view.InventoryViews.ReorderRecommendationView;
import SA.irms.inventory.domain.ReorderRecommendation;
import SA.irms.inventory.domain.ReorderRecommendationStrategy;
import SA.irms.inventory.domain.StockLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcReorderRecommendationQueryRepository {
    private final JdbcClient jdbcClient;
    private final ReorderRecommendationStrategy reorderRecommendationStrategy;

    JdbcReorderRecommendationQueryRepository(
            JdbcClient jdbcClient,
            ReorderRecommendationStrategy reorderRecommendationStrategy
    ) {
        this.jdbcClient = jdbcClient;
        this.reorderRecommendationStrategy = reorderRecommendationStrategy;
    }

    List<ReorderRecommendationView> loadRecommendations() {
        return jdbcClient.sql("""
                        select i.inventory_item_id, i.name, rr.threshold, rr.target_qty, rr.lead_time_days, i.on_hand
                        from reorder_rules rr
                        join inventory_items i on i.inventory_item_id = rr.inventory_item_id
                        where rr.is_active = true
                        order by i.name
                        """)
                .query((rs, rowNum) -> toView(reorderRecommendationStrategy.compute(new StockLevel(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("threshold"),
                        rs.getBigDecimal("target_qty"),
                        rs.getInt("lead_time_days"),
                        rs.getBigDecimal("on_hand")
                ))))
                .list();
    }

    private ReorderRecommendationView toView(ReorderRecommendation recommendation) {
        return new ReorderRecommendationView(
                recommendation.inventoryItemId(),
                recommendation.name(),
                recommendation.threshold(),
                recommendation.targetQty(),
                recommendation.onHand(),
                recommendation.averageUsage(),
                recommendation.projectedNeed(),
                recommendation.recommendedOrderQty(),
                recommendation.confidence()
        );
    }
}
