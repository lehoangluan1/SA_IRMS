package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.port.out.ReservationTableCommandPort;

@Repository
public class JdbcReservationTableCommandAdapter implements ReservationTableCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcReservationTableCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID createTable(UUID branchId, int number, int capacity, String sectionLabel) {
        UUID tableId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into dining_tables (
                            table_id,
                            branch_id,
                            code,
                            capacity,
                            zone,
                            floor_label,
                            section_label,
                            position_row,
                            position_col,
                            status
                        ) values (
                            :tableId,
                            :branchId,
                            :code,
                            :capacity,
                            'Main Hall',
                            'Ground',
                            :sectionLabel,
                            0,
                            0,
                            'available'
                        )
                        """)
                .param("tableId", tableId)
                .param("branchId", branchId)
                .param("code", "T" + number)
                .param("capacity", capacity)
                .param("sectionLabel", sectionLabel)
                .update();
        return tableId;
    }

    @Override
    public void updateTable(UUID tableId, int number, int capacity, String sectionLabel) {
        jdbcClient.sql("""
                        update dining_tables
                        set code = :code,
                            capacity = :capacity,
                            section_label = :sectionLabel,
                            updated_at = now()
                        where table_id = :tableId
                        """)
                .param("code", "T" + number)
                .param("capacity", capacity)
                .param("sectionLabel", sectionLabel)
                .param("tableId", tableId)
                .update();
    }

    @Override
    public long countActiveSessions(UUID tableId) {
        return jdbcClient.sql("""
                        select count(*)
                        from table_sessions
                        where table_id = :tableId
                          and status in ('active', 'billing')
                        """)
                .param("tableId", tableId)
                .query(Long.class)
                .single();
    }

    @Override
    public int deleteTable(UUID tableId) {
        return jdbcClient.sql("delete from dining_tables where table_id = :tableId")
                .param("tableId", tableId)
                .update();
    }

    @Override
    public void closeActiveSession(UUID sessionId) {
        jdbcClient.sql("""
                        update table_sessions
                        set status = 'closed',
                            closed_at = now(),
                            updated_at = now()
                        where session_id = :sessionId
                        """)
                .param("sessionId", sessionId)
                .update();
    }

    @Override
    public void releaseAssignmentsByTable(UUID tableId, String reason) {
        jdbcClient.sql("""
                        update table_assignments
                        set released_at = now(),
                            released_reason = :reason,
                            updated_at = now()
                        where table_id = :tableId
                          and released_at is null
                        """)
                .param("tableId", tableId)
                .param("reason", reason)
                .update();
    }

    @Override
    public void updateTableStatus(UUID tableId, String targetStatus) {
        jdbcClient.sql("""
                        update dining_tables
                        set status = :targetStatus,
                            updated_at = now()
                        where table_id = :tableId
                        """)
                .param("targetStatus", targetStatus)
                .param("tableId", tableId)
                .update();
    }
}
