package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.port.out.OrderRepository;

final class JdbcPromotionCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcPromotionCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void createPromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, Timestamp effectiveTo) {
        jdbcClient.sql("""
                        insert into promotion_campaigns (promotion_campaign_id, code, name, discount_type, discount_value,
                                                         effective_from, effective_to, is_active)
                        values (:promotionId, :code, :name, :discountType, :discountValue, now(), :effectiveTo, true)
                        """)
                .param("promotionId", promotionId)
                .param("code", code)
                .param("name", name)
                .param("discountType", discountType)
                .param("discountValue", discountValue)
                .param("effectiveTo", effectiveTo)
                .update();
    }

    boolean updatePromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, Timestamp effectiveTo) {
        return jdbcClient.sql("""
                        update promotion_campaigns
                        set code = :code, name = :name, discount_type = :discountType, discount_value = :discountValue,
                            effective_to = :effectiveTo, updated_at = now()
                        where promotion_campaign_id = :promotionId
                        """)
                .param("code", code)
                .param("name", name)
                .param("discountType", discountType)
                .param("discountValue", discountValue)
                .param("effectiveTo", effectiveTo)
                .param("promotionId", promotionId)
                .update() == 1;
    }

    boolean deletePromotion(UUID promotionId) {
        return jdbcClient.sql("delete from promotion_campaigns where promotion_campaign_id = :promotionId")
                .param("promotionId", promotionId)
                .update() == 1;
    }

    Optional<OrderRepository.PromotionRow> findActivePromotionByCode(String code) {
        return jdbcClient.sql("""
                        select promotion_campaign_id, discount_type, discount_value
                        from promotion_campaigns
                        where upper(code) = upper(:code)
                          and is_active = true
                          and (effective_to is null or effective_to >= now())
                        """)
                .param("code", code)
                .query((rs, rowNum) -> new OrderRepository.PromotionRow(
                        rs.getObject("promotion_campaign_id", UUID.class),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value")
                ))
                .optional();
    }

    Optional<BigDecimal> findBillSubtotal(UUID billId) {
        return jdbcClient.sql("select sub_total from bills where bill_id = :billId")
                .param("billId", billId)
                .query(BigDecimal.class)
                .optional();
    }

    void applyPromotionToBill(UUID billId, UUID promotionId, String code, BigDecimal discount) {
        jdbcClient.sql("""
                        update bills
                        set discount_total = :discount,
                            promotion_campaign_id = :promotionCampaignId,
                            promotion_code = :code,
                            grand_total = sub_total + tax_amount + service_fee + tip_amount - :discount,
                            updated_at = now()
                        where bill_id = :billId
                        """)
                .param("discount", discount)
                .param("promotionCampaignId", promotionId)
                .param("code", code)
                .param("billId", billId)
                .update();
    }
}
