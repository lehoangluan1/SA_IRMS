package SA.irms.common.config;

import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import SA.irms.billing.domain.AmountSplitStrategy;
import SA.irms.billing.domain.BillSplitStrategy;
import SA.irms.billing.domain.EqualSplitStrategy;
import SA.irms.billing.domain.ItemSplitStrategy;
import SA.irms.billing.domain.SeatSplitStrategy;
import SA.irms.inventory.domain.LowStockSeverityPolicy;
import SA.irms.inventory.domain.ReorderRecommendationStrategy;
import SA.irms.inventory.domain.UsageBasedReorderRecommendationStrategy;
import SA.irms.kitchen.domain.KitchenPriorityPolicy;
import SA.irms.kitchen.domain.KitchenStatusPolicy;
import SA.irms.kitchen.domain.service.KitchenDeadlinePolicy;
import SA.irms.kitchen.domain.service.KitchenTicketTransitionPolicy;
import SA.irms.ordering.domain.ComboPricingPolicy;
import SA.irms.ordering.domain.OrderItemStatusPolicy;
import SA.irms.ordering.domain.OrderStatusPolicy;

@Configuration
public class DomainPolicyConfiguration {
    @Bean
    public OrderStatusPolicy orderStatusPolicy() {
        return new OrderStatusPolicy();
    }

    @Bean
    public ComboPricingPolicy comboPricingPolicy() {
        return new ComboPricingPolicy();
    }

    @Bean
    public OrderItemStatusPolicy orderItemStatusPolicy() {
        return new OrderItemStatusPolicy();
    }

    @Bean
    public KitchenStatusPolicy kitchenStatusPolicy() {
        return new KitchenStatusPolicy();
    }

    @Bean
    public KitchenPriorityPolicy kitchenPriorityPolicy() {
        return new KitchenPriorityPolicy();
    }

    @Bean
    public KitchenTicketTransitionPolicy kitchenTicketTransitionPolicy() {
        return new KitchenTicketTransitionPolicy();
    }

    @Bean
    public KitchenDeadlinePolicy kitchenDeadlinePolicy(Clock clock) {
        return new KitchenDeadlinePolicy(clock);
    }

    @Bean
    public ReorderRecommendationStrategy reorderRecommendationStrategy() {
        return new UsageBasedReorderRecommendationStrategy();
    }

    @Bean
    public LowStockSeverityPolicy lowStockSeverityPolicy() {
        return new LowStockSeverityPolicy();
    }

    @Bean
    public BillSplitStrategy amountSplitStrategy() {
        return new AmountSplitStrategy();
    }

    @Bean
    public BillSplitStrategy equalSplitStrategy() {
        return new EqualSplitStrategy();
    }

    @Bean
    public BillSplitStrategy itemSplitStrategy() {
        return new ItemSplitStrategy();
    }

    @Bean
    public BillSplitStrategy seatSplitStrategy() {
        return new SeatSplitStrategy();
    }

    @Bean
    public List<BillSplitStrategy> billSplitStrategies(
            BillSplitStrategy amountSplitStrategy,
            BillSplitStrategy equalSplitStrategy,
            BillSplitStrategy itemSplitStrategy,
            BillSplitStrategy seatSplitStrategy
    ) {
        return List.of(amountSplitStrategy, equalSplitStrategy, itemSplitStrategy, seatSplitStrategy);
    }
}
