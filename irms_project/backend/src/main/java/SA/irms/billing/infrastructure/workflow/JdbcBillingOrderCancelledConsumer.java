package SA.irms.billing.infrastructure.workflow;

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
public class JdbcBillingOrderCancelledConsumer {
    public static final String CONSUMER_NAME = "billing.order-cancelled.adjustment";

    private final ManualAckConsumerSupport ackSupport;
    private final JdbcClient jdbcClient;

    public JdbcBillingOrderCancelledConsumer(ManualAckConsumerSupport ackSupport, JdbcClient jdbcClient) {
        this.ackSupport = ackSupport;
        this.jdbcClient = jdbcClient;
    }

    @RabbitListener(queues = "irms.billing.order-cancelled.q")
    public void consume(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, CONSUMER_NAME, this::voidOpenBillIfUnpaid);
    }

    @Transactional
    void voidOpenBillIfUnpaid(EventEnvelope envelope) {
        java.util.UUID orderId = java.util.UUID.fromString(envelope.metadata().aggregateId());
        jdbcClient.sql("""
                        update bills b
                        set status = 'void', updated_at = now(), closed_at = coalesce(closed_at, now())
                        from orders o
                        where o.table_session_id = b.table_session_id
                          and o.order_id = :orderId
                          and b.status in ('open', 'finalized')
                        """)
                .param("orderId", orderId)
                .update();
    }
}
