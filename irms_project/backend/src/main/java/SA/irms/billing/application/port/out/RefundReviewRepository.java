package SA.irms.billing.application.port.out;

import java.util.UUID;

public interface RefundReviewRepository {
    void markRejected(UUID refundId, UUID approvedBy);
}
