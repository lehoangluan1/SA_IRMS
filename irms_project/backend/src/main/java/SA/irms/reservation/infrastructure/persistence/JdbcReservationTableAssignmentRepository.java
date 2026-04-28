package SA.irms.reservation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.query.TableCandidate;
import SA.irms.reservation.application.port.out.ReservationTableAssignmentRepository;

@Repository
public class JdbcReservationTableAssignmentRepository implements ReservationTableAssignmentRepository {
    private final JdbcClient jdbcClient;

    public JdbcReservationTableAssignmentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int countActiveWaitlistEntries() {
        return jdbcClient.sql("""
                        select count(*)
                        from waitlist_entries
                        where status in ('waiting', 'notified')
                        """)
                .query(Integer.class)
                .single();
    }

    @Override
    public List<TableCandidate> loadTablesByMinimumCapacity(int partySize) {
        return jdbcClient.sql("""
                        select table_id, code, capacity, status
                        from dining_tables
                        where capacity >= :partySize
                        order by capacity, code
                        """)
                .param("partySize", partySize)
                .query((rs, rowNum) -> new TableCandidate(
                        rs.getObject("table_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("code")),
                        rs.getInt("capacity"),
                        rs.getString("status")
                ))
                .list();
    }

    @Override
    public Optional<UUID> findAssignedTableId(UUID reservationId) {
        return jdbcClient.sql("""
                        select table_id
                        from table_assignments
                        where reservation_id = :reservationId
                          and released_at is null
                        order by assigned_at desc
                        limit 1
                        """)
                .param("reservationId", reservationId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public void releaseSpecificAssignment(UUID reservationId, UUID tableId, String reason) {
        jdbcClient.sql("""
                        update table_assignments
                        set released_at = now(),
                            released_reason = :reason,
                            updated_at = now()
                        where reservation_id = :reservationId
                          and table_id = :tableId
                          and released_at is null
                        """)
                .param("reservationId", reservationId)
                .param("tableId", tableId)
                .param("reason", reason)
                .update();
    }

    @Override
    public void markTableAvailableIfReserved(UUID tableId) {
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'available',
                            updated_at = now()
                        where table_id = :tableId
                          and status = 'reserved'
                        """)
                .param("tableId", tableId)
                .update();
    }

    @Override
    public void reserveTableForReservation(UUID reservationId, UUID tableId) {
        long count = jdbcClient.sql("""
                        select count(*)
                        from table_assignments
                        where reservation_id = :reservationId
                          and table_id = :tableId
                          and released_at is null
                        """)
                .param("reservationId", reservationId)
                .param("tableId", tableId)
                .query(Long.class)
                .single();
        if (count == 0) {
            jdbcClient.sql("""
                            insert into table_assignments (
                                assignment_id,
                                reservation_id,
                                table_id,
                                assigned_at,
                                reason,
                                score
                            ) values (
                                :assignmentId,
                                :reservationId,
                                :tableId,
                                now(),
                                'reservation_match',
                                90
                            )
                            """)
                    .param("assignmentId", UUID.randomUUID())
                    .param("reservationId", reservationId)
                    .param("tableId", tableId)
                    .update();
        }
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'reserved',
                            updated_at = now()
                        where table_id = :tableId
                        """)
                .param("tableId", tableId)
                .update();
    }
}
