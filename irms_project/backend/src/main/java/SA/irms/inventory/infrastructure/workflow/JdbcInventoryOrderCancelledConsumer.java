package SA.irms.inventory.infrastructure.workflow;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import com.rabbitmq.client.Channel;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class JdbcInventoryOrderCancelledConsumer {
    public static final String CONSUMER_NAME = "inventory.order-cancelled.compensation";

    private final ManualAckConsumerSupport ackSupport;
    private final JdbcClient jdbcClient;

    public JdbcInventoryOrderCancelledConsumer(ManualAckConsumerSupport ackSupport, JdbcClient jdbcClient) {
        this.ackSupport = ackSupport;
        this.jdbcClient = jdbcClient;
    }

    @RabbitListener(queues = "irms.inventory.order-cancelled.q")
    public void consume(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, CONSUMER_NAME, this::releaseReservations);
    }

    @Transactional
    void releaseReservations(EventEnvelope envelope) {
        java.util.UUID orderId = java.util.UUID.fromString(envelope.metadata().aggregateId());
        jdbcClient.sql("""
                        update inventory_reservations
                        set status = 'cancelled', cancelled_at = now()
                        where source_order_id = :orderId
                          and status = 'held'
                        """)
                .param("orderId", orderId)
                .update();
    }
}
