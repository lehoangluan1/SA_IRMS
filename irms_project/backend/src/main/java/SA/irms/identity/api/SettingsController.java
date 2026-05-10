package SA.irms.identity.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.identity.application.SettingsService;
import SA.irms.identity.application.command.IdentityCommands;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final PermissionGuard permissionGuard;

    public SettingsController(
            SettingsService settingsService,
            PermissionGuard permissionGuard
    ) {
        this.settingsService = settingsService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<Map<String, Object>> settings(HttpServletRequest request) {
        permissionGuard.require("settings.view");
        return ApiEnvelope.of(settingsService.loadSettings(), RequestContext.getCorrelationId(request));
    }

    @PatchMapping
    public ApiEnvelope<Map<String, Object>> updateSettings(
            @Valid @RequestBody SettingsUpdateBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("settings.manage");
        return ApiEnvelope.of(settingsService.updateSettings(requestBody.toRequest(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request));
    }

    public record SettingsUpdateBody(
            @Min(1) int waitlistHoldMinutes,
            @Min(0) int reservationGraceMinutes,
            @Min(0) int kitchenRushThresholdMinutes,
            @Min(0) int kitchenLateThresholdMinutes,
            @Min(1) int refundWindowHours
    ) {
        IdentityCommands.UpdateSettingsRequest toRequest() {
            return new IdentityCommands.UpdateSettingsRequest(
                    waitlistHoldMinutes,
                    reservationGraceMinutes,
                    kitchenRushThresholdMinutes,
                    kitchenLateThresholdMinutes,
                    refundWindowHours
            );
        }
    }
}
