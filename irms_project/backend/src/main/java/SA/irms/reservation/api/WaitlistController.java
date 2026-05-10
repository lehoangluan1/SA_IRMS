package SA.irms.reservation.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.reservation.application.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {
    private final ReservationService reservationService;
    private final PermissionGuard permissionGuard;

    public WaitlistController(ReservationService reservationService, PermissionGuard permissionGuard) {
        this.reservationService = reservationService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.WaitlistView> createWaitlist(@Valid @RequestBody WaitlistBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.createWaitlistEntry(requestBody.toUpsert(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{waitlistEntryId}/notify")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.WaitlistView> notifyWaitlist(@PathVariable UUID waitlistEntryId, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.notifyWaitlist(waitlistEntryId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{waitlistEntryId}/skip")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.WaitlistView> skipWaitlist(@PathVariable UUID waitlistEntryId, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.skipWaitlist(waitlistEntryId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{waitlistEntryId}/prioritize")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.WaitlistView> prioritizeWaitlist(@PathVariable UUID waitlistEntryId, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.prioritizeWaitlist(waitlistEntryId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{waitlistEntryId}/seat")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.WaitlistView> seatWaitlist(@PathVariable UUID waitlistEntryId, HttpServletRequest request) {
        var actor = permissionGuard.require("tables.assign");
        return ApiEnvelope.of(reservationService.seatWaitlist(waitlistEntryId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record WaitlistBody(@NotBlank String name, @NotBlank String phone, String email, String notes, @NotNull Integer party) {
        SA.irms.reservation.application.command.ReservationCommands.WaitlistUpsert toUpsert() {
            return new SA.irms.reservation.application.command.ReservationCommands.WaitlistUpsert(name, phone, email, notes, party);
        }
    }
}
