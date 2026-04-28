package SA.irms.identity.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.json.JsonSupport;

@Repository
public class IdentityReportingRepository {
    private final JdbcClient jdbcClient;
    private final JsonSupport jsonSupport;

    public IdentityReportingRepository(JdbcClient jdbcClient, JsonSupport jsonSupport) {
        this.jdbcClient = jdbcClient;
        this.jsonSupport = jsonSupport;
    }

    public Optional<IdentityRepository.ReportSnapshotRow> loadLatestReportSnapshot(String type) {
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
                .param("type", type)
                .query((rs, rowNum) -> new IdentityRepository.ReportSnapshotRow(
                        rs.getObject("snapshot_id", UUID.class),
                        rs.getString("type"),
                        timestamp(rs.getTimestamp("period_start")),
                        timestamp(rs.getTimestamp("period_end")),
                        timestamp(rs.getTimestamp("generated_at")),
                        jsonSupport.readMap(rs.getString("payload"))
                ))
                .optional();
    }

    private Instant timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
