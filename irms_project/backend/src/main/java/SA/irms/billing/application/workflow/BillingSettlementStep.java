package SA.irms.billing.application.workflow;

import SA.irms.common.events.EventEnvelope;

public interface BillingSettlementStep {
    String name();

    boolean supports(EventEnvelope event);

    void execute(EventEnvelope event);
}
