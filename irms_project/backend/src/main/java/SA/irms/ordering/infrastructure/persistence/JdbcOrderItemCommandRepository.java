package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.port.out.OrderRepository;

final class JdbcOrderItemCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcOrderItemCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void replaceModifierSelections(UUID orderItemId, List<OrderRepository.ModifierSelectionRow> selectedOptions) {
        jdbcClient.sql("delete from order_item_modifiers where order_item_id = :orderItemId")
                .param("orderItemId", orderItemId)
                .update();
        insertModifierSelections(orderItemId, selectedOptions);
    }

    void insertModifierSelections(UUID orderItemId, List<OrderRepository.ModifierSelectionRow> selectedOptions) {
        for (OrderRepository.ModifierSelectionRow option : selectedOptions) {
            jdbcClient.sql("""
                            insert into order_item_modifiers (
                                selection_id,
                                order_item_id,
                                modifier_option_id,
                                name_snapshot,
                                extra_price,
                                qty_multiplier
                            ) values (
                                :selectionId,
                                :orderItemId,
                                :modifierOptionId,
                                :nameSnapshot,
                                :extraPrice,
                                1
                            )
                            """)
                    .param("selectionId", UUID.randomUUID())
                    .param("orderItemId", orderItemId)
                    .param("modifierOptionId", option.optionId())
                    .param("nameSnapshot", option.name())
                    .param("extraPrice", option.extraPrice())
                    .update();
        }
    }

    void createOrderItem(UUID orderId, UUID orderItemId, UUID menuItemId, String snapshotName, int quantity, BigDecimal unitPrice,
                         String specialInstruction, String allergyNotes, String lineStatus, boolean draft, boolean sendLater) {
        jdbcClient.sql("""
                        insert into order_items (order_item_id, order_id, menu_item_id, snapshot_name, quantity, unit_price,
                                                 special_instruction, allergy_notes, line_status, fire_at, sent_to_kitchen_at)
                        values (:orderItemId, :orderId, :menuItemId, :snapshotName, :quantity, :unitPrice,
                                :specialInstruction, :allergyNotes, :lineStatus,
                                case when :draft or :sendLater then null else now() end,
                                case when :draft or :sendLater then null else now() end)
                        """)
                .param("orderItemId", orderItemId)
                .param("orderId", orderId)
                .param("menuItemId", menuItemId)
                .param("snapshotName", snapshotName)
                .param("quantity", quantity)
                .param("unitPrice", unitPrice)
                .param("specialInstruction", specialInstruction)
                .param("allergyNotes", allergyNotes)
                .param("lineStatus", lineStatus)
                .param("draft", draft)
                .param("sendLater", sendLater)
                .update();
    }

    void updateDraftOrderItem(UUID orderItemId, String snapshotName, BigDecimal unitPrice) {
        jdbcClient.sql("""
                        update order_items
                        set snapshot_name = :snapshotName, unit_price = :unitPrice, updated_at = now()
                        where order_item_id = :orderItemId
                        """)
                .param("snapshotName", snapshotName)
                .param("unitPrice", unitPrice)
                .param("orderItemId", orderItemId)
                .update();
    }

    void updateOrderLineStatus(UUID orderId, UUID orderItemId, String lineStatus) {
        jdbcClient.sql("""
                        update order_items
                        set line_status = :lineStatus,
                            served_at = case when :lineStatus = 'served' then coalesce(served_at, now()) else served_at end,
                            updated_at = now()
                        where order_id = :orderId
                          and order_item_id = :orderItemId
                          and line_status <> 'cancelled'
                        """)
                .param("lineStatus", lineStatus)
                .param("orderId", orderId)
                .param("orderItemId", orderItemId)
                .update();
    }

    Optional<OrderRepository.OrderItemServedState> findServedState(UUID orderItemId) {
        return jdbcClient.sql("""
                        select line_status, served_at
                        from order_items
                        where order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new OrderRepository.OrderItemServedState(
                        rs.getString("line_status"),
                        rs.getTimestamp("served_at") == null ? null : rs.getTimestamp("served_at").toInstant()
                ))
                .optional();
    }

    void markOrderItemServed(UUID orderItemId) {
        jdbcClient.sql("""
                        update order_items
                        set line_status = 'served',
                            served_at = coalesce(served_at, now()),
                            updated_at = now()
                        where order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .update();
    }

    void markOrderItemDelayed(UUID orderItemId) {
        jdbcClient.sql("""
                        update order_items
                        set line_status = 'hold_for_service',
                            fire_at = null,
                            updated_at = now()
                        where order_item_id = :orderItemId
                          and line_status not in ('served', 'cancelled')
                        """)
                .param("orderItemId", orderItemId)
                .update();
    }

    void markOrderItemSentToKitchen(UUID orderItemId) {
        jdbcClient.sql("""
                        update order_items
                        set line_status = 'sent_to_kitchen',
                            sent_to_kitchen_at = coalesce(sent_to_kitchen_at, now()),
                            fire_at = now(),
                            updated_at = now()
                        where order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .update();
    }

    void cancelOrderItem(UUID orderItemId, String reason) {
        jdbcClient.sql("""
                        update order_items
                        set line_status = 'cancelled',
                            cancellation_reason = :reason,
                            cancelled_at = now(),
                            updated_at = now()
                        where order_item_id = :orderItemId
                        """)
                .param("reason", reason)
                .param("orderItemId", orderItemId)
                .update();
    }

    int cancelActiveOrderItems(UUID orderId, String reason) {
        return jdbcClient.sql("""
                        update order_items
                        set line_status = 'cancelled',
                            cancellation_reason = :reason,
                            cancelled_at = coalesce(cancelled_at, now()),
                            updated_at = now()
                        where order_id = :orderId
                          and line_status not in ('served', 'cancelled')
                        """)
                .param("orderId", orderId)
                .param("reason", reason)
                .update();
    }
}
