package SA.irms.notification.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.error.ConflictException;
import SA.irms.notification.application.port.out.NotificationChannelAdapter;
import SA.irms.notification.application.port.out.NotificationQueuePort;
import SA.irms.common.notification.NotificationCommand;

@Repository
public class JdbcNotificationQueueRepository implements NotificationQueuePort {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final Map<String, NotificationChannelAdapter> adapters;

    public JdbcNotificationQueueRepository(
            JdbcClient jdbcClient,
            ObjectMapper objectMapper,
            List<NotificationChannelAdapter> adapters
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(
                        adapter -> adapter.channel().toLowerCase(),
                        Function.identity()
                ));
    }

    @Override
    @Transactional
    public QueuedNotification queue(NotificationCommand command) {
        String channel = normalizeChannel(command.channel());
        NotificationChannelAdapter adapter = adapters.get(channel);
        if (adapter == null) {
            throw new ConflictException("Unsupported notification channel.");
        }
        adapter.validate(command);

        UUID messageId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcClient.sql("""
                        insert into notification_messages (
                            message_id,
                            low_stock_alert_id,
                            reservation_id,
                            waitlist_entry_id,
                            payment_id,
                            channel,
                            type,
                            template_code,
                            payload,
                            title,
                            body,
                            recipient_role,
                            recipient_user_id,
                            recipient_address,
                            priority,
                            status,
                            created_at,
                            queued_at,
                            sent_at
                        ) values (
                            :messageId,
                            :lowStockAlertId,
                            :reservationId,
                            :waitlistEntryId,
                            :paymentId,
                            :channel,
                            :type,
                            :templateCode,
                            cast(:payload as jsonb),
                            :title,
                            :body,
                            :recipientRole,
                            :recipientUserId,
                            :recipientAddress,
                            :priority,
                            'queued',
                            :createdAt,
                            :queuedAt,
                            null
                        )
                        """)
                .param("messageId", messageId)
                .param("lowStockAlertId", command.lowStockAlertId())
                .param("reservationId", command.reservationId())
                .param("waitlistEntryId", command.waitlistEntryId())
                .param("paymentId", command.paymentId())
                .param("channel", channel)
                .param("type", command.type())
                .param("templateCode", command.templateCode())
                .param("payload", toJson(command.payload()))
                .param("title", command.title())
                .param("body", command.body())
                .param("recipientRole", command.recipientRole())
                .param("recipientUserId", command.recipientUserId())
                .param("recipientAddress", command.recipientAddress())
                .param("priority", normalizePriority(command.priority()))
                .param("createdAt", now)
                .param("queuedAt", now)
                .update();

        jdbcClient.sql("""
                        insert into notification_deliveries (
                            delivery_id,
                            message_id,
                            channel,
                            delivery_status,
                            attempt,
                            created_at,
                            updated_at
                        ) values (
                            :deliveryId,
                            :messageId,
                            :channel,
                            'QUEUED',
                            1,
                            :createdAt,
                            :createdAt
                        )
                        """)
                .param("deliveryId", UUID.randomUUID())
                .param("messageId", messageId)
                .param("channel", channel)
                .param("createdAt", now)
                .update();

        return new QueuedNotification(messageId, channel, "queued");
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new ConflictException("A notification channel is required.");
        }
        return channel.trim().toLowerCase();
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "medium";
        }
        String normalized = priority.trim().toLowerCase();
        if (!List.of("low", "medium", "high", "critical").contains(normalized)) {
            throw new ConflictException("Unsupported notification priority.");
        }
        return normalized;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Notification payload could not be serialized.", exception);
        }
    }
}
