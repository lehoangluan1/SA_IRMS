package SA.irms.reporting.application;

import org.springframework.stereotype.Component;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.common.events.EventEnvelope;

@Component
public class StaffEfficiencyProjectionHandler implements ReportingProjectionHandler {
    private final ReportingProjectionRepository repository;

    public StaffEfficiencyProjectionHandler(ReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(EventEnvelope envelope) {
        return "StaffShiftChanged".equals(envelope.metadata().eventType())
                || envelope.payload().containsKey("staffId")
                || envelope.payload().containsKey("serverId");
    }

    @Override
    public void project(EventEnvelope envelope) {
        repository.materializeStaffEfficiencyProjection(envelope);
    }
}
