package SA.irms.reservation.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.StatusResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.reservation.application.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/notifications")
public class ReservationNotificationController {
    private final ReservationService reservationService;
    private final PermissionGuard permissionGuard;

    public ReservationNotificationController(ReservationService reservationService, PermissionGuard permissionGuard) {
        this.reservationService = reservationService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping
    public ApiEnvelope<StatusResponse> sendNotification(@Valid @RequestBody NotificationBody requestBody, HttpServletRequest request) {
        permissionGuard.require("reservations.manage");
        reservationService.sendNotification(requestBody.reservationId(), requestBody.waitlistEntryId(), requestBody.title(), requestBody.body(), requestBody.channel());
        return ApiEnvelope.of(new StatusResponse("queued"), RequestContext.getCorrelationId(request));
    }

    public record NotificationBody(UUID reservationId, UUID waitlistEntryId, @NotBlank String title, @NotBlank String body, @NotBlank String channel) {
    }
}
