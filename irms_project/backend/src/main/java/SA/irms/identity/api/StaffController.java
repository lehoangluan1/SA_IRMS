package SA.irms.identity.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.identity.application.StaffService;
import SA.irms.identity.application.command.IdentityCommands;
import SA.irms.identity.application.view.IdentityViews;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
@Validated
public class StaffController {
    private final StaffService staffService;
    private final PermissionGuard permissionGuard;

    public StaffController(StaffService staffService, PermissionGuard permissionGuard) {
        this.staffService = staffService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/staff")
    public ApiEnvelope<IdentityViews.StaffView> staff(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "shiftDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            HttpServletRequest request
    ) {
        permissionGuard.require("staff.manage");
        return ApiEnvelope.of(staffService.loadStaff(search, shiftDate), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/shifts")
    public ApiEnvelope<IdentityViews.ShiftView> createShift(
            @Valid @RequestBody CreateShiftBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("staff.manage");
        return ApiEnvelope.of(
                staffService.createShift(
                        new IdentityCommands.CreateShiftRequest(
                                requestBody.userId(),
                                requestBody.shiftDate(),
                                requestBody.startAt(),
                                requestBody.endAt(),
                                requestBody.position(),
                                requestBody.zone()
                        ),
                        actor,
                        RequestContext.getCorrelationId(request),
                        RequestContext.metadata(request)
                ),
                RequestContext.getCorrelationId(request)
        );
    }

    @PatchMapping("/staff/{userId}/roles")
    public ApiEnvelope<IdentityViews.StaffRowView> updateRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRolesBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("staff.manage");
        return ApiEnvelope.of(
                staffService.updateRoles(
                        new IdentityCommands.UpdateRolesRequest(userId, requestBody.roleIds()),
                        actor,
                        RequestContext.getCorrelationId(request),
                        RequestContext.metadata(request)
                ),
                RequestContext.getCorrelationId(request)
        );
    }

    public record CreateShiftBody(
            @NotNull java.util.UUID userId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @NotNull java.time.LocalTime startAt,
            @NotNull java.time.LocalTime endAt,
            @NotBlank String position,
            String zone
    ) {
    }

    public record UpdateRolesBody(@NotEmpty List<@NotNull UUID> roleIds) {
    }
}
