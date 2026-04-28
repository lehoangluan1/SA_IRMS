package SA.irms.billing.application.workflow;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class PaymentCompletedConsumer {
    public static final String PAYMENT_CONSUMER_NAME = "billing.payment-completed.settlement";
    public static final String REFUND_CONSUMER_NAME = "billing.refund-issued.settlement";

    private final ManualAckConsumerSupport ackSupport;
    private final BillingSettlementMediator mediator;

    public PaymentCompletedConsumer(ManualAckConsumerSupport ackSupport, BillingSettlementMediator mediator) {
        this.ackSupport = ackSupport;
        this.mediator = mediator;
    }

    @RabbitListener(queues = "irms.billing.receipt.q")
    public void paymentCompleted(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, PAYMENT_CONSUMER_NAME, mediator::handlePaymentCompleted);
    }

    @RabbitListener(queues = "irms.billing.refund-settlement.q")
    public void refundIssued(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, REFUND_CONSUMER_NAME, mediator::handleRefundIssued);
    }
}
