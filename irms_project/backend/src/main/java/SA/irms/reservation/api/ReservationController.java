package SA.irms.reservation.api;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    private final PermissionGuard permissionGuard;

    public ReservationController(ReservationService reservationService, PermissionGuard permissionGuard) {
        this.reservationService = reservationService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/overview")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationOverview> overview(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, HttpServletRequest request) {
        permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.load(date), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/recommendations")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationRecommendation> recommendations(@RequestParam("date") String date, @RequestParam("time") String time, @RequestParam("party") Integer party, HttpServletRequest request) {
        permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.recommend(date, time, party), RequestContext.getCorrelationId(request));
    }

    @PostMapping
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationView> createReservation(@Valid @RequestBody ReservationBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.createReservation(requestBody.toUpsert(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/{reservationId}")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationView> updateReservation(@PathVariable UUID reservationId, @Valid @RequestBody ReservationEditBody requestBody, HttpServletRequest request) {
        permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.updateReservation(reservationId, requestBody.toPatch()), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{reservationId}/confirm")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationView> confirmReservation(@PathVariable UUID reservationId, HttpServletRequest request) {
        permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.confirmReservation(reservationId), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{reservationId}/check-in")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationView> checkIn(@PathVariable UUID reservationId, @RequestBody(required = false) CheckInBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("tables.assign");
        SA.irms.reservation.application.command.ReservationCommands.CheckInRequest checkInRequest = requestBody == null ? new SA.irms.reservation.application.command.ReservationCommands.CheckInRequest(null, null, null) : requestBody.toRequest();
        return ApiEnvelope.of(reservationService.checkIn(reservationId, checkInRequest, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{reservationId}/no-show")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.ReservationView> noShow(@PathVariable UUID reservationId, HttpServletRequest request) {
        var actor = permissionGuard.require("reservations.manage");
        return ApiEnvelope.of(reservationService.markNoShow(reservationId, RequestContext.getCorrelationId(request), actor, RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record ReservationBody(@NotBlank String guest, @NotBlank String phone, String email, @NotNull Integer party, @NotBlank String date, @NotBlank String time, String notes, UUID tableId, Boolean fallbackToWaitlist) {
        SA.irms.reservation.application.command.ReservationCommands.ReservationUpsert toUpsert() {
            return new SA.irms.reservation.application.command.ReservationCommands.ReservationUpsert(guest, phone, email, party, date, time, notes, tableId, Boolean.TRUE.equals(fallbackToWaitlist));
        }
    }

    public record ReservationEditBody(@NotBlank String guest, @NotNull Integer party, String notes) {
        SA.irms.reservation.application.command.ReservationCommands.ReservationPatch toPatch() {
            return new SA.irms.reservation.application.command.ReservationCommands.ReservationPatch(guest, party, notes);
        }
    }

    public record CheckInBody(Boolean approveRecovery, Integer actualPartySize, UUID replacementTableId) {
        SA.irms.reservation.application.command.ReservationCommands.CheckInRequest toRequest() {
            return new SA.irms.reservation.application.command.ReservationCommands.CheckInRequest(approveRecovery, actualPartySize, replacementTableId);
        }
    }
}
