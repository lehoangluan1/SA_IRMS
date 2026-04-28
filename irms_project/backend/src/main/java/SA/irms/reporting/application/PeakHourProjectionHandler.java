package SA.irms.reporting.application;

import org.springframework.stereotype.Component;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

@Component
public class PeakHourProjectionHandler implements ReportingProjectionHandler {
    private final ReportingProjectionRepository repository;

    public PeakHourProjectionHandler(ReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.ORDER_CONFIRMED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.RESERVATION_CREATED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.RESERVATION_SEATED.equals(envelope.metadata().eventType());
    }

    @Override
    public void project(EventEnvelope envelope) {
        repository.materializePeakHourProjection(envelope);
    }
}
