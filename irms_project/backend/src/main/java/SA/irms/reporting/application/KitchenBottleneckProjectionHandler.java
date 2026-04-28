package SA.irms.reporting.application;

import org.springframework.stereotype.Component;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

@Component
public class KitchenBottleneckProjectionHandler implements ReportingProjectionHandler {
    private final ReportingProjectionRepository repository;

    public KitchenBottleneckProjectionHandler(ReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.KITCHEN_TICKET_CREATED.equals(envelope.metadata().eventType());
    }

    @Override
    public void project(EventEnvelope envelope) {
        repository.materializeKitchenBottleneckProjection(envelope);
    }
}
