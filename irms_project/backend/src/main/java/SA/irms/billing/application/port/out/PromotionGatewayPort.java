package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PromotionGatewayPort {
    BigDecimal applyPromotion(UUID billId, String code);
}
