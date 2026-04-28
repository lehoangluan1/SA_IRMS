package SA.irms.reporting.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotPayload;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotQuery;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotSummaryView;

final class JdbcReportSnapshotCrudRepository extends ReportingProjectionJdbcSupport {
    private final ReportSnapshotPayloadMapper payloadMapper;

    JdbcReportSnapshotCrudRepository(JdbcClient jdbcClient, ReportSnapshotPayloadMapper payloadMapper) {
        super(jdbcClient);
        this.payloadMapper = payloadMapper;
    }

    Optional<DynamicReportSnapshot> findSnapshot(UUID snapshotId) {
        return jdbcClient.sql("""
                        select snapshot_id,
                               type,
                               period_start,
                               period_end,
                               generated_at,
                               payload::text as payload
                        from report_snapshots
                        where snapshot_id = :snapshotId
                        """)
                .param("snapshotId", snapshotId)
                .query((rs, rowNum) -> new DynamicReportSnapshot(
                        rs.getObject("snapshot_id", UUID.class),
                        rs.getString("type"),
                        timestamp(rs.getTimestamp("period_start")),
                        timestamp(rs.getTimestamp("period_end")),
                        timestamp(rs.getTimestamp("generated_at")),
                        payloadMapper.snapshotPayload(rs.getString("type"), rs.getString("payload"))
                ))
                .optional();
    }

    Optional<DynamicReportSnapshot> findLatestSnapshot(String reportCode) {
        return jdbcClient.sql("""
                        select snapshot_id,
                               type,
                               period_start,
                               period_end,
                               generated_at,
                               payload::text as payload
                        from report_snapshots
                        where type = :type
                        order by generated_at desc
                        limit 1
                        """)
                .param("type", reportCode)
                .query((rs, rowNum) -> new DynamicReportSnapshot(
                        rs.getObject("snapshot_id", UUID.class),
                        rs.getString("type"),
                        timestamp(rs.getTimestamp("period_start")),
                        timestamp(rs.getTimestamp("period_end")),
                        timestamp(rs.getTimestamp("generated_at")),
                        payloadMapper.snapshotPayload(rs.getString("type"), rs.getString("payload"))
                ))
                .optional();
    }

    List<ReportSnapshotSummaryView> findSnapshots(ReportSnapshotQuery query) {
        String reportCode = query.reportCode() == null || query.reportCode().isBlank() ? null : query.reportCode();
        Instant generatedAfter = query.generatedAfter() == null ? Instant.EPOCH : query.generatedAfter();
        Instant generatedBefore = query.generatedBefore() == null ? Instant.now() : query.generatedBefore();
        return jdbcClient.sql("""
                        select snapshot_id,
                               type,
                               period_start,
                               period_end,
                               generated_at,
                               payload::text as payload
                        from report_snapshots
                        where (:reportCode is null or type = :reportCode)
                          and generated_at >= :generatedAfter
                          and generated_at <= :generatedBefore
                        order by generated_at desc
                        limit :limitValue
                        """)
                .param("reportCode", reportCode)
                .param("generatedAfter", Timestamp.from(generatedAfter))
                .param("generatedBefore", Timestamp.from(generatedBefore))
                .param("limitValue", query.limit())
                .query((rs, rowNum) -> {
                    ReportSnapshotPayload payload = payloadMapper.snapshotPayload(rs.getString("type"), rs.getString("payload"));
                    return new ReportSnapshotSummaryView(
                            rs.getObject("snapshot_id", UUID.class),
                            rs.getString("type"),
                            payload.reportView().reportName(),
                            timestamp(rs.getTimestamp("generated_at")),
                            timestamp(rs.getTimestamp("period_start")),
                            timestamp(rs.getTimestamp("period_end"))
                    );
                })
                .list();
    }

    void saveSnapshot(DynamicReportSnapshot snapshot) {
        jdbcClient.sql("""
                        insert into report_snapshots (
                            snapshot_id,
                            type,
                            period_start,
                            period_end,
                            generated_at,
                            payload
                        ) values (
                            :snapshotId,
                            :type,
                            :periodStart,
                            :periodEnd,
                            :generatedAt,
                            cast(:payload as jsonb)
                        )
                        on conflict (snapshot_id) do update
                        set type = excluded.type,
                            period_start = excluded.period_start,
                            period_end = excluded.period_end,
                            generated_at = excluded.generated_at,
                            payload = excluded.payload
                        """)
                .param("snapshotId", snapshot.snapshotId())
                .param("type", snapshot.reportCode())
                .param("periodStart", Timestamp.from(snapshot.periodStart()))
                .param("periodEnd", Timestamp.from(snapshot.periodEnd()))
                .param("generatedAt", Timestamp.from(snapshot.generatedAt()))
                .param("payload", payloadMapper.snapshotToJson(snapshot.payload()))
                .update();
    }
}
