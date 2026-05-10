package SA.irms.kitchen.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.ConflictException;

@Repository
class JdbcKitchenRoutePlanRepository {
    private final JdbcClient jdbcClient;

    JdbcKitchenRoutePlanRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    UUID createRoutePlan(UUID orderId, String notes) {
        UUID routePlanId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into route_plans (route_plan_id, created_from_order_id, notes)
                        values (:routePlanId, :orderId, :notes)
                        """)
                .param("routePlanId", routePlanId)
                .param("orderId", orderId)
                .param("notes", notes)
                .update();
        return routePlanId;
    }

    UUID resolveStationId(String stationKind) {
        return jdbcClient.sql("""
                        select station_id
                        from kitchen_stations
                        where kind = :stationKind
                        limit 1
                        """)
                .param("stationKind", stationKind)
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new ConflictException("Kitchen station routing is not configured for " + stationKind + "."));
    }

    void deleteEmptyRoutePlan(UUID routePlanId) {
        jdbcClient.sql("""
                        delete from route_plans rp
                        where rp.route_plan_id = :routePlanId
                          and not exists (
                              select 1
                              from kitchen_tickets kt
                              where kt.route_plan_id = rp.route_plan_id
                          )
                        """)
                .param("routePlanId", routePlanId)
                .update();
    }
}
