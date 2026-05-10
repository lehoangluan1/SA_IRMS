package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.support.MenuJsonSupport;
import SA.irms.ordering.application.view.OrderViews;

final class JdbcTableSessionQueryRepository {
    private final JdbcClient jdbcClient;
    private final MenuJsonSupport menuJsonSupport;
    private final JdbcModifierQueryRepository modifierQueryRepository;

    JdbcTableSessionQueryRepository(JdbcClient jdbcClient, MenuJsonSupport menuJsonSupport, JdbcModifierQueryRepository modifierQueryRepository) {
        this.jdbcClient = jdbcClient;
        this.menuJsonSupport = menuJsonSupport;
        this.modifierQueryRepository = modifierQueryRepository;
    }

    List<OrderViews.TableSessionView> loadTableSessions() {
        return jdbcClient.sql("""
                        select ts.session_id, dt.code, ts.guest_count
                        from table_sessions ts
                        join dining_tables dt on dt.table_id = ts.table_id
                        where ts.status in ('active', 'billing')
                        order by dt.code
                        """)
                .query((rs, rowNum) -> new OrderViews.TableSessionView(
                        rs.getObject("session_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("code")),
                        rs.getInt("guest_count")
                ))
                .list();
    }

    List<OrderViews.MenuItemView> loadMenuItemsForOrdering() {
        return jdbcClient.sql("""
                        select m.menu_item_id, m.name, m.base_price, c.name as category_name,
                               m.station, m.availability, m.allergens_json::text as allergens_json
                        from menu_items m
                        join menu_categories c on c.category_id = m.category_id
                        order by c.display_order, m.name
                        """)
                .query((rs, rowNum) -> {
                    UUID menuItemId = rs.getObject("menu_item_id", UUID.class);
                    return new OrderViews.MenuItemView(
                            menuItemId,
                            rs.getString("name"),
                            rs.getBigDecimal("base_price"),
                            rs.getString("category_name"),
                            rs.getString("station"),
                            "available".equals(rs.getString("availability")),
                            menuJsonSupport.parseJsonArray(rs.getString("allergens_json")),
                            modifierQueryRepository.loadModifierGroups(menuItemId)
                    );
                })
                .list();
    }
}
