package SA.irms.kitchen.events;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class KitchenWorkflowEventHandler {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public KitchenWorkflowEventHandler(
            JdbcClient jdbcClient,
            ObjectMapper objectMapper
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleKitchenDishStatusChanged(KitchenDishStatusChangedEvent event) {
        jdbcClient.sql("""
                        insert into dish_status_events (
                            event_id,
                            ticket_item_id,
                            handoff_id,
                            type,
                            source_station,
                            details,
                            occurred_at
                        ) values (
                            :eventId,
                            :ticketItemId,
                            :handoffId,
                            :type,
                            :sourceStation,
                            cast(:details as jsonb),
                            now()
                        )
                        """)
                .param("eventId", UUID.randomUUID())
                .param("ticketItemId", event.ticketItemId())
                .param("handoffId", event.handoffId())
                .param("type", event.type())
                .param("sourceStation", event.sourceStation())
                .param("details", toJson(event.details()))
                .update();
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Dish status details could not be serialized.", exception);
        }
    }
}
