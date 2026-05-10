package SA.irms.inventory.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.inventory.application.port.out.InventoryStockTransactionRepository;
import SA.irms.inventory.application.events.InventoryStockChangedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Service
public class InventoryStockTransactionService {
    private final InventoryStockTransactionRepository repository;
    private final Clock clock;
    private final DomainEventPublisher outboxEventPublisher;

    public InventoryStockTransactionService(InventoryStockTransactionRepository repository, Clock clock, DomainEventPublisher outboxEventPublisher) {
        this.repository = repository;
        this.clock = clock;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    public boolean recordTransaction(UUID inventoryItemId, BigDecimal delta, String reason, UUID actorUserId, String correlationId, UUID sourceRef, String sourceType) {
        BigDecimal previousOnHand = repository.lockCurrentOnHand(inventoryItemId);
        if (repository.hasRecordedSource(inventoryItemId, reason, sourceRef, sourceType)) {
            return false;
        }
        BigDecimal newOnHand = previousOnHand.add(delta);
        UUID transactionId = UUID.randomUUID();
        repository.updateOnHand(inventoryItemId, newOnHand);
        repository.insertTransaction(transactionId, inventoryItemId, delta, reason, sourceRef, sourceType,
                previousOnHand, newOnHand, actorUserId, correlationId, Instant.now(clock));
        publishStockChanged(transactionId, inventoryItemId, delta, reason, sourceRef, sourceType, previousOnHand, newOnHand, correlationId);
        return true;
    }

    private void publishStockChanged(UUID transactionId, UUID inventoryItemId, BigDecimal delta, String reason,
            UUID sourceRef, String sourceType, BigDecimal previousOnHand, BigDecimal newOnHand, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transactionId", transactionId.toString());
        payload.put("inventoryItemId", inventoryItemId.toString());
        payload.put("delta", delta);
        payload.put("reason", reason);
        payload.put("previousOnHand", previousOnHand);
        payload.put("newOnHand", newOnHand);
        if (sourceRef != null) {
            payload.put("sourceRef", sourceRef.toString());
        }
        if (sourceType != null) {
            payload.put("sourceType", sourceType);
        }
        outboxEventPublisher.publish(new InventoryStockChangedEvent(inventoryItemId.toString(), payload), correlationId, null);
    }
}
