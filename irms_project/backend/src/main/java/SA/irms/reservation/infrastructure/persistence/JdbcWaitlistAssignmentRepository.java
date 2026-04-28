package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcWaitlistAssignmentRepository {
    private final JdbcClient jdbcClient;

    JdbcWaitlistAssignmentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void createWaitlistTableAssignment(UUID waitlistEntryId, UUID tableId, UUID tableSessionId) {
        jdbcClient.sql("""
                        insert into table_assignments (
                            assignment_id,
                            waitlist_entry_id,
                            table_id,
                            table_session_id,
                            assigned_at,
                            reason,
                            score
                        ) values (
                            :assignmentId,
                            :waitlistEntryId,
                            :tableId,
                            :tableSessionId,
                            now(),
                            'waitlist_match',
                            80
                        )
                        """)
                .param("assignmentId", UUID.randomUUID())
                .param("waitlistEntryId", waitlistEntryId)
                .param("tableId", tableId)
                .param("tableSessionId", tableSessionId)
                .update();
    }
}
