package SA.irms.reservation.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.EntityReferenceResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.reservation.application.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/tables")
public class TableController {
    private final ReservationService reservationService;
    private final PermissionGuard permissionGuard;

    public TableController(ReservationService reservationService, PermissionGuard permissionGuard) {
        this.reservationService = reservationService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.TableView> createTable(@Valid @RequestBody TableBody requestBody, HttpServletRequest request) {
        permissionGuard.require("tables.assign");
        return ApiEnvelope.of(reservationService.createTable(requestBody.toUpsert()), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/{tableId}")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.TableView> updateTable(@PathVariable UUID tableId, @Valid @RequestBody TableBody requestBody, HttpServletRequest request) {
        permissionGuard.require("tables.assign");
        return ApiEnvelope.of(reservationService.updateTable(tableId, requestBody.toUpsert()), RequestContext.getCorrelationId(request));
    }

    @DeleteMapping("/{tableId}")
    public ApiEnvelope<EntityReferenceResponse> deleteTable(@PathVariable UUID tableId, HttpServletRequest request) {
        permissionGuard.require("tables.assign");
        reservationService.deleteTable(tableId);
        return ApiEnvelope.of(new EntityReferenceResponse("table", tableId), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{tableId}/status")
    public ApiEnvelope<SA.irms.reservation.application.view.ReservationViews.TableActionResult> updateTableStatus(@PathVariable UUID tableId, @Valid @RequestBody TableStatusBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("tables.assign");
        return ApiEnvelope.of(reservationService.updateTableStatus(tableId, requestBody.toRequest(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record TableBody(@NotNull Integer number, @NotNull Integer capacity, String notes) {
        SA.irms.reservation.application.command.ReservationCommands.TableUpsert toUpsert() {
            return new SA.irms.reservation.application.command.ReservationCommands.TableUpsert(number, capacity, notes);
        }
    }

    public record TableStatusBody(@NotBlank String targetStatus, String reason) {
        SA.irms.reservation.application.command.ReservationCommands.TableStatusUpdate toRequest() {
            return new SA.irms.reservation.application.command.ReservationCommands.TableStatusUpdate(targetStatus, reason);
        }
    }
}
