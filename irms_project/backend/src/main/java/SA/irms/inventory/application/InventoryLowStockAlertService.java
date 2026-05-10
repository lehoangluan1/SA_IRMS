package SA.irms.inventory.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.inventory.application.port.out.InventoryLowStockAlertRepository;
import SA.irms.inventory.domain.LowStockSeverityPolicy;
import SA.irms.common.notification.NotificationCommand;
import SA.irms.common.notification.NotificationCommandPublisher;

@Service
public class InventoryLowStockAlertService {
    private final InventoryLowStockAlertRepository repository;
    private final NotificationCommandPublisher notificationOutboxPublisher;
    private final LowStockSeverityPolicy lowStockSeverityPolicy;

    public InventoryLowStockAlertService(
            InventoryLowStockAlertRepository repository,
            NotificationCommandPublisher notificationOutboxPublisher,
            LowStockSeverityPolicy lowStockSeverityPolicy) {
        this.repository = repository;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.lowStockSeverityPolicy = lowStockSeverityPolicy;
    }

    @Transactional
    public void acknowledgeAlert(UUID alertId, UUID actorUserId) {
        repository.acknowledgeAlert(alertId, actorUserId);
    }

    @Transactional
    public void evaluateLowStock(UUID inventoryItemId, String correlationId) {
        InventoryLowStockAlertRepository.LowStockSnapshot item = repository.findSnapshot(inventoryItemId).orElse(null);
        if (item == null)
            return;

        boolean isCurrentlyLow = item.current().compareTo(item.minimum()) <= 0;
        boolean hasOpenAlert = repository.hasOpenAlert(inventoryItemId);

        if (isCurrentlyLow && !hasOpenAlert) {
            // TRƯỜNG HỢP 1: Stock thấp mà chưa có alert -> Tạo mới
            UUID alertId = repository.createOpenAlert(
                    inventoryItemId,
                    lowStockSeverityPolicy.alertSeverity(item.current(), item.minimum()));
            queueLowStockNotifications(alertId, item, correlationId);
        } else if (!isCurrentlyLow && hasOpenAlert) {
            // TRƯỜNG HỢP 2: Stock đã OK nhưng vẫn còn alert open -> Tự động đóng (Resolve)
            repository.resolveOpenAlerts(inventoryItemId);
        }
    }

    private void queueLowStockNotifications(UUID alertId, InventoryLowStockAlertRepository.LowStockSnapshot item,
            String correlationId) {
        String priority = lowStockSeverityPolicy.notificationPriority(item.current(), item.minimum());
        String body = item.name() + " is below the configured threshold. Review affected dishes and reorder guidance.";
        for (String recipientRole : List.of("manager", "chef")) {
            notificationOutboxPublisher.enqueue(new NotificationCommand(
                    alertId,
                    null,
                    null,
                    null,
                    "in_app",
                    "low_stock",
                    "LOW_STOCK_ALERT",
                    Map.of("inventoryItemId", item.inventoryItemId().toString()),
                    "Low stock alert",
                    body,
                    recipientRole,
                    null,
                    null,
                    priority), "LowStockAlert", alertId.toString(), correlationId);
        }
    }
}
