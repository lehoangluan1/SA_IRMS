package SA.irms.identity.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityStaffRepositoryPort {
    List<StaffRow> findStaff(String search);

    Optional<StaffRow> findStaffById(UUID userId);

    List<RoleRow> findRoles();

    long countActiveUsersWithRole(String roleName);

    List<ShiftRow> findShifts(LocalDate shiftDate);

    Optional<ShiftRow> findShiftById(UUID shiftAssignmentId);

    UUID createShift(UUID userId, UUID branchId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt, String position, String zone);

    long countShiftConflicts(UUID userId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt);

    void replaceUserRoles(UUID userId, List<UUID> roleIds);

    record StaffRow(UUID userId, String displayName, String email, String status, LocalDate hireDate, Set<String> roles, Set<String> permissions) {
    }

    record RoleRow(UUID roleId, String name, String scope, String description, List<String> permissions) {
    }

    record ShiftRow(
            UUID shiftAssignmentId,
            UUID userId,
            String displayName,
            String roleName,
            LocalDate shiftDate,
            LocalTime startAt,
            LocalTime endAt,
            String position,
            String zone,
            String status
    ) {
    }
}
