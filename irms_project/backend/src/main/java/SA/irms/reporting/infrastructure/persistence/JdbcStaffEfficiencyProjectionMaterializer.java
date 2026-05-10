package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;

final class JdbcStaffEfficiencyProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcStaffEfficiencyProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        String staffId = stringValue(envelope.payload().get("staffId"), stringValue(envelope.payload().get("serverId"), null));
        if (staffId == null) {
            return;
        }
        LocalDate businessDate = businessDate(envelope);
        String role = stringValue(envelope.payload().get("role"), envelope.payload().containsKey("serverId") ? "server" : "staff");
        java.math.BigDecimal averageServiceTime = decimal(envelope.payload().getOrDefault("serviceTimeSeconds", 0));
        jdbcClient.sql("""
                        insert into reporting_staff_efficiency_projection (
                            business_date, staff_id, role, completed_tasks, average_service_time
                        ) values (
                            :businessDate, :staffId, :role, 1, :averageServiceTime
                        )
                        on conflict (business_date, staff_id) do update
                        set completed_tasks = reporting_staff_efficiency_projection.completed_tasks + 1,
                            average_service_time = ((reporting_staff_efficiency_projection.average_service_time * reporting_staff_efficiency_projection.completed_tasks) + excluded.average_service_time)
                                                    / (reporting_staff_efficiency_projection.completed_tasks + 1),
                            role = excluded.role,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("staffId", staffId)
                .param("role", role)
                .param("averageServiceTime", averageServiceTime)
                .update();
    }
}
