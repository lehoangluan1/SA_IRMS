package SA.irms.identity.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class IdentityShiftRepository {
    private final JdbcClient jdbcClient;

    IdentityShiftRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<IdentityRepository.ShiftRow> findShifts(LocalDate shiftDate) {
        return jdbcClient.sql("""
                        select s.shift_assignment_id,
                               s.user_id,
                               u.display_name,
                               r.name as role_name,
                               s.shift_date,
                               s.start_at,
                               s.end_at,
                               s.position,
                               s.zone,
                               s.status
                        from shift_assignments s
                        join users u on u.user_id = s.user_id
                        left join user_roles ur on ur.user_id = u.user_id
                        left join roles r on r.role_id = ur.role_id
                        where s.shift_date = :shiftDate
                        order by s.start_at, u.display_name
                        """)
                .param("shiftDate", shiftDate)
                .query((rs, rowNum) -> new IdentityRepository.ShiftRow(
                        rs.getObject("shift_assignment_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("role_name"),
                        rs.getDate("shift_date").toLocalDate(),
                        rs.getTime("start_at").toLocalTime(),
                        rs.getTime("end_at").toLocalTime(),
                        rs.getString("position"),
                        rs.getString("zone"),
                        rs.getString("status")
                ))
                .list();
    }

    java.util.Optional<IdentityRepository.ShiftRow> findShiftById(UUID shiftAssignmentId) {
        return jdbcClient.sql("""
                        select s.shift_assignment_id,
                               s.user_id,
                               u.display_name,
                               r.name as role_name,
                               s.shift_date,
                               s.start_at,
                               s.end_at,
                               s.position,
                               s.zone,
                               s.status
                        from shift_assignments s
                        join users u on u.user_id = s.user_id
                        left join user_roles ur on ur.user_id = u.user_id
                        left join roles r on r.role_id = ur.role_id
                        where s.shift_assignment_id = :shiftAssignmentId
                        """)
                .param("shiftAssignmentId", shiftAssignmentId)
                .query((rs, rowNum) -> new IdentityRepository.ShiftRow(
                        rs.getObject("shift_assignment_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("role_name"),
                        rs.getDate("shift_date").toLocalDate(),
                        rs.getTime("start_at").toLocalTime(),
                        rs.getTime("end_at").toLocalTime(),
                        rs.getString("position"),
                        rs.getString("zone"),
                        rs.getString("status")
                ))
                .optional();
    }

    @Transactional
    UUID createShift(UUID userId, UUID branchId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt, String position, String zone) {
        UUID shiftAssignmentId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into shift_assignments (
                            shift_assignment_id,
                            user_id,
                            branch_id,
                            shift_date,
                            start_at,
                            end_at,
                            position,
                            zone,
                            status
                        ) values (
                            :shiftAssignmentId,
                            :userId,
                            :branchId,
                            :shiftDate,
                            :startAt,
                            :endAt,
                            :position,
                            :zone,
                            'scheduled'
                        )
                        """)
                .param("shiftAssignmentId", shiftAssignmentId)
                .param("userId", userId)
                .param("branchId", branchId)
                .param("shiftDate", shiftDate)
                .param("startAt", startAt)
                .param("endAt", endAt)
                .param("position", position)
                .param("zone", zone)
                .update();
        return shiftAssignmentId;
    }

    long countShiftConflicts(UUID userId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt) {
        return jdbcClient.sql("""
                        select count(*)
                        from shift_assignments
                        where user_id = :userId
                          and shift_date = :shiftDate
                          and status = 'scheduled'
                          and start_at < :endAt
                          and end_at > :startAt
                        """)
                .param("userId", userId)
                .param("shiftDate", shiftDate)
                .param("startAt", startAt)
                .param("endAt", endAt)
                .query(Long.class)
                .single();
    }
}
