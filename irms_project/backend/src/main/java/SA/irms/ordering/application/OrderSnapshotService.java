package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.ordering.application.port.out.OrderRepository;

@Service
class OrderSnapshotService {
    private final OrderRepository repository;

    OrderSnapshotService(OrderRepository repository) {
        this.repository = repository;
    }

    void upsertOrderSnapshot(UUID orderId, BigDecimal subtotal, Instant pricedAt) {
        repository.updateOrderSnapshot(orderId, subtotal, pricedAt);
    }

    Timestamp toSqlTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
