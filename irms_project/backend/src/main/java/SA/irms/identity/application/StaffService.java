package SA.irms.identity.application;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.identity.audit.AuditService;
import SA.irms.identity.application.command.IdentityCommands;
import SA.irms.identity.application.view.IdentityViews;
import SA.irms.identity.application.port.out.IdentityPolicyRepositoryPort;
import SA.irms.identity.application.port.out.IdentityStaffRepositoryPort;
import SA.irms.identity.application.port.out.IdentityUserRepository;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.context.RequestMetadata;
import SA.irms.common.notification.NotificationCommand;

@Service
public class StaffService {
    private final IdentityStaffRepositoryPort identityStaffRepository;
    private final IdentityPolicyRepositoryPort identityPolicyRepository;
    private final IdentityUserRepository identityUserRepository;
    private final AuditService auditService;
    private final NotificationCommandPublisher notificationOutboxPublisher;

    public StaffService(
            IdentityStaffRepositoryPort identityStaffRepository,
            IdentityPolicyRepositoryPort identityPolicyRepository,
            IdentityUserRepository identityUserRepository,
            AuditService auditService,
            NotificationCommandPublisher notificationOutboxPublisher
    ) {
        this.identityStaffRepository = identityStaffRepository;
        this.identityPolicyRepository = identityPolicyRepository;
        this.identityUserRepository = identityUserRepository;
        this.auditService = auditService;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
    }

    public IdentityViews.StaffView loadStaff(String search, LocalDate shiftDate) {
        List<IdentityStaffRepositoryPort.StaffRow> staff = identityStaffRepository.findStaff(search);
        List<IdentityStaffRepositoryPort.RoleRow> roles = identityStaffRepository.findRoles();
        List<IdentityStaffRepositoryPort.ShiftRow> shifts = identityStaffRepository.findShifts(shiftDate == null ? LocalDate.now() : shiftDate);
        return new IdentityViews.StaffView(staff, roles, shifts);
    }

    @Transactional
    public IdentityViews.ShiftView createShift(
            IdentityCommands.CreateShiftRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new ConflictException("Shift end time must be after the start time.");
        }
        long conflictCount = identityStaffRepository.countShiftConflicts(
                request.userId(),
                request.shiftDate(),
                request.startAt(),
                request.endAt()
        );
        if (conflictCount > 0) {
            throw new ConflictException("The selected staff member already has an overlapping shift.");
        }

        UUID shiftId = identityStaffRepository.createShift(
                request.userId(),
                identityPolicyRepository.findDefaultBranch().branchId(),
                request.shiftDate(),
                request.startAt(),
                request.endAt(),
                request.position(),
                request.zone()
        );
        IdentityStaffRepositoryPort.ShiftRow createdShift = identityStaffRepository.findShiftById(shiftId)
                .orElseThrow(() -> new NotFoundException("The created shift could not be loaded."));

        auditService.record(
                actor.userId(),
                "staff.shift.created",
                "ShiftAssignment",
                shiftId.toString(),
                correlationId,
                null,
                false,
                httpServletRequest.remoteIp(),
                java.util.Map.of(),
                java.util.Map.of(
                        "userId", request.userId(),
                        "shiftDate", request.shiftDate().toString(),
                        "startAt", request.startAt().toString(),
                        "endAt", request.endAt().toString(),
                        "position", request.position()
                )
        );

        notificationOutboxPublisher.enqueue(new NotificationCommand(
                null,
                null,
                null,
                null,
                "in_app",
                "staff_schedule",
                "SHIFT_ASSIGNED",
                java.util.Map.of(
                        "shiftAssignmentId", shiftId.toString(),
                        "shiftDate", request.shiftDate().toString(),
                        "startAt", request.startAt().toString(),
                        "endAt", request.endAt().toString(),
                        "position", request.position(),
                        "zone", request.zone() == null ? "" : request.zone()
                ),
                "Shift assigned",
                "A new shift has been assigned for " + request.shiftDate() + " (" + request.startAt() + "-" + request.endAt() + ").",
                null,
                request.userId(),
                createdShift.displayName(),
                "medium"
        ), "ShiftAssignment", shiftId.toString(), correlationId);

        return toShiftView(createdShift);
    }

    @Transactional
    public IdentityViews.StaffRowView updateRoles(
            IdentityCommands.UpdateRolesRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new ConflictException("At least one role must remain assigned.");
        }

        IdentityStaffRepositoryPort.StaffRow existing = identityStaffRepository.findStaffById(request.userId())
                .orElseThrow(() -> new NotFoundException("The selected staff member was not found."));

        List<IdentityStaffRepositoryPort.RoleRow> availableRoles = identityStaffRepository.findRoles();
        Set<UUID> validRoleIds = availableRoles.stream()
                .map(IdentityStaffRepositoryPort.RoleRow::roleId)
                .collect(Collectors.toSet());
        if (!validRoleIds.containsAll(request.roleIds())) {
            throw new ConflictException("One or more selected roles are invalid.");
        }

        Set<String> updatedRoleNames = availableRoles.stream()
                .filter(role -> request.roleIds().contains(role.roleId()))
                .map(IdentityStaffRepositoryPort.RoleRow::name)
                .collect(Collectors.toSet());

        if (existing.roles().contains("admin")
                && !updatedRoleNames.contains("admin")
                && identityStaffRepository.countActiveUsersWithRole("admin") <= 1) {
            throw new ConflictException("The last active administrator role cannot be removed.");
        }

        identityStaffRepository.replaceUserRoles(request.userId(), request.roleIds());
        identityUserRepository.terminateActiveSessionsForUser(request.userId(), Instant.now());

        IdentityStaffRepositoryPort.StaffRow updated = identityStaffRepository.findStaffById(request.userId())
                .orElseThrow(() -> new NotFoundException("The updated staff record could not be loaded."));

        auditService.record(
                actor.userId(),
                "iam.role.modified",
                "User",
                request.userId().toString(),
                correlationId,
                "Roles were updated from the staff management screen.",
                true,
                httpServletRequest.remoteIp(),
                java.util.Map.of("roles", existing.roles()),
                java.util.Map.of("roles", updated.roles(), "requiresRelogin", true)
        );

        return toStaffRowView(updated);
    }

    private IdentityViews.ShiftView toShiftView(IdentityStaffRepositoryPort.ShiftRow row) {
        return new IdentityViews.ShiftView(row.shiftAssignmentId(), row.userId(), row.displayName(), row.roleName(), row.shiftDate(), row.startAt(), row.endAt(), row.position(), row.zone(), row.status());
    }

    private IdentityViews.StaffRowView toStaffRowView(IdentityStaffRepositoryPort.StaffRow row) {
        return new IdentityViews.StaffRowView(row.userId(), row.displayName(), row.email(), row.status(), row.hireDate(), row.roles(), row.permissions());
    }
}
