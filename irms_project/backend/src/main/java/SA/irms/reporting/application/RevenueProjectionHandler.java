package SA.irms.reporting.application;

import org.springframework.stereotype.Component;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

@Component
public class RevenueProjectionHandler implements ReportingProjectionHandler {
    private final ReportingProjectionRepository repository;

    public RevenueProjectionHandler(ReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.RECEIPT_GENERATED.equals(envelope.metadata().eventType());
    }

    @Override
    public void project(EventEnvelope envelope) {
        repository.materializeRevenueProjection(envelope);
    }
}
