package SA.irms.reporting.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.config.AppProperties;
import SA.irms.reporting.application.port.out.ReportingProjectionRecorderPort;
import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.support.OperationsSnapshotViewMapper;
import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotPayload;
import SA.irms.common.events.EventEnvelope;

@Service
public class ReportingProjectionService implements ReportingProjectionRecorderPort {
    private final ReportingProjectionRepository repository;
    private final List<ReportingProjectionHandler> projectionHandlers;
    private final AppProperties appProperties;
    private final OperationsSnapshotViewMapper operationsSnapshotViewMapper;
    private final Clock clock;

    public ReportingProjectionService(
            ReportingProjectionRepository repository,
            List<ReportingProjectionHandler> projectionHandlers,
            AppProperties appProperties,
            OperationsSnapshotViewMapper operationsSnapshotViewMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.projectionHandlers = List.copyOf(projectionHandlers);
        this.appProperties = appProperties;
        this.operationsSnapshotViewMapper = operationsSnapshotViewMapper;
        this.clock = clock;
    }

    @Transactional
    public void recordEventProjection(EventEnvelope envelope) {
        boolean handled = false;
        for (ReportingProjectionHandler handler : projectionHandlers) {
            if (handler.supports(envelope)) {
                handler.project(envelope);
                handled = true;
            }
        }
        if (!handled) {
            repository.recordEventProjection(envelope);
        }
        refreshOperationsSnapshotIfDue(envelope.metadata().eventId());
    }

    public void refreshOperationsSnapshotIfDue(UUID outboxEventId) {
        if (repository.alreadyRefreshedForEvent(outboxEventId)) {
            return;
        }
        Instant now = Instant.now(clock);
        Instant latest = repository.findLatestSnapshot("operations")
                .map(DynamicReportSnapshot::generatedAt)
                .orElse(null);
        if (latest != null && Duration.between(latest, now).getSeconds() < appProperties.reporting().projectionRefreshSeconds()) {
            return;
        }
        refreshOperationsSnapshot(now, outboxEventId);
    }

    @Transactional
    public void refreshOperationsSnapshot(Instant generatedAt, UUID outboxEventId) {
        ReportingProjectionRepository.OperationsMetrics metrics = repository.loadOperationsMetrics();
        DynamicReportSnapshot existingSnapshot = repository.findLatestSnapshot("operations").orElse(null);
        OperationsReportView report = operationsSnapshotViewMapper.compose(
                existingSnapshot,
                metrics,
                generatedAt,
                outboxEventId == null ? null : outboxEventId.toString()
        );
        repository.saveSnapshot(new DynamicReportSnapshot(
                UUID.randomUUID(),
                "operations",
                report.periodStart(),
                report.periodEnd(),
                report.generatedAt(),
                ReportSnapshotPayload.fromOperationsReport(report)
        ));
    }
}
