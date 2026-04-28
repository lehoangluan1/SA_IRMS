package SA.irms.identity.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.identity.application.port.out.IdentityStaffRepositoryPort;

@Repository
public class IdentityStaffRepository implements IdentityStaffRepositoryPort {
    private final IdentityStaffReadRepository staffReadRepository;
    private final IdentityRoleRepository roleRepository;
    private final IdentityShiftRepository shiftRepository;

    public IdentityStaffRepository(
            IdentityStaffReadRepository staffReadRepository,
            IdentityRoleRepository roleRepository,
            IdentityShiftRepository shiftRepository
    ) {
        this.staffReadRepository = staffReadRepository;
        this.roleRepository = roleRepository;
        this.shiftRepository = shiftRepository;
    }

    @Override
    public List<StaffRow> findStaff(String search) {
        return staffReadRepository.findStaff(search).stream().map(this::toStaffRow).toList();
    }

    @Override
    public java.util.Optional<StaffRow> findStaffById(UUID userId) {
        return staffReadRepository.findStaffById(userId).map(this::toStaffRow);
    }

    @Override
    public List<RoleRow> findRoles() {
        return roleRepository.findRoles().stream().map(this::toRoleRow).toList();
    }

    @Override
    public long countActiveUsersWithRole(String roleName) {
        return roleRepository.countActiveUsersWithRole(roleName);
    }

    @Override
    public List<ShiftRow> findShifts(LocalDate shiftDate) {
        return shiftRepository.findShifts(shiftDate).stream().map(this::toShiftRow).toList();
    }

    @Override
    public java.util.Optional<ShiftRow> findShiftById(UUID shiftAssignmentId) {
        return shiftRepository.findShiftById(shiftAssignmentId).map(this::toShiftRow);
    }

    @Transactional
    @Override
    public UUID createShift(UUID userId, UUID branchId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt, String position, String zone) {
        return shiftRepository.createShift(userId, branchId, shiftDate, startAt, endAt, position, zone);
    }

    @Override
    public long countShiftConflicts(UUID userId, LocalDate shiftDate, LocalTime startAt, LocalTime endAt) {
        return shiftRepository.countShiftConflicts(userId, shiftDate, startAt, endAt);
    }

    @Transactional
    @Override
    public void replaceUserRoles(UUID userId, List<UUID> roleIds) {
        roleRepository.replaceUserRoles(userId, roleIds);
    }

    private StaffRow toStaffRow(IdentityRepository.StaffRow row) {
        return new StaffRow(row.userId(), row.displayName(), row.email(), row.status(), row.hireDate(), row.roles(), row.permissions());
    }

    private RoleRow toRoleRow(IdentityRepository.RoleRow row) {
        return new RoleRow(row.roleId(), row.name(), row.scope(), row.description(), row.permissions());
    }

    private ShiftRow toShiftRow(IdentityRepository.ShiftRow row) {
        return new ShiftRow(row.shiftAssignmentId(), row.userId(), row.displayName(), row.roleName(), row.shiftDate(), row.startAt(), row.endAt(), row.position(), row.zone(), row.status());
    }

}