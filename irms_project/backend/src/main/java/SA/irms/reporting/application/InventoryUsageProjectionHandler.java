package SA.irms.reporting.application;

import org.springframework.stereotype.Component;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

@Component
public class InventoryUsageProjectionHandler implements ReportingProjectionHandler {
    private final ReportingProjectionRepository repository;

    public InventoryUsageProjectionHandler(ReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.INVENTORY_STOCK_CHANGED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.LOW_STOCK_DETECTED.equals(envelope.metadata().eventType());
    }

    @Override
    public void project(EventEnvelope envelope) {
        repository.materializeInventoryUsageProjection(envelope);
    }
}
