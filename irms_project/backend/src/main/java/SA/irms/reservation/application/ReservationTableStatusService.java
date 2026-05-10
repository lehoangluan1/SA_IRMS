package SA.irms.reservation.application;

import SA.irms.reservation.application.query.ActiveTableState;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.reservation.application.port.out.ReservationTableCommandPort;
import SA.irms.reservation.application.port.out.TableQueryRepository;

@Service
public class ReservationTableStatusService {
    private final ReservationTableCommandPort tableCommandPort;
    private final TableQueryRepository tableReadService;
    private final AuditRecorder auditService;

    public ReservationTableStatusService(
            ReservationTableCommandPort tableCommandPort,
            TableQueryRepository tableReadService,
            AuditRecorder auditService
    ) {
        this.tableCommandPort = tableCommandPort;
        this.tableReadService = tableReadService;
        this.auditService = auditService;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableActionResult updateTableStatus(
            UUID tableId,
            SA.irms.reservation.application.command.ReservationCommands.TableStatusUpdate request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata requestMetadata
    ) {
        SA.irms.reservation.application.view.ReservationViews.TableView existing = tableReadService.findTable(tableId);
        String targetStatus = normalizeTableStatus(request.targetStatus());
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Updated from the table management screen."
                : request.reason().trim();
        ActiveTableState activeTableState = tableReadService.loadActiveTableState(tableId);
        if ("available".equals(targetStatus) && !"cleaning".equals(existing.status())) {
            throw new ConflictException("Only a table in cleaning status can be marked available.");
        }
        if (activeTableState.hasActiveSession() && !"cleaning".equals(targetStatus)) {
            throw new ConflictException("Release the active table session to cleaning before applying this status.");
        }
        String action = "table.status.updated";
        String message = "Table status updated.";
        String suggestedGuest = null;
        if ("cleaning".equals(targetStatus) && activeTableState.hasActiveSession()) {
            if (activeTableState.hasOutstandingBalance()) {
                throw new ConflictException("The table cannot be released until the related bill and bill splits are settled.");
            }
            tableCommandPort.closeActiveSession(activeTableState.sessionId());
            tableCommandPort.releaseAssignmentsByTable(tableId, reason);
            action = "table.released";
            message = "Table released to cleaning.";
        }
        tableCommandPort.updateTableStatus(tableId, targetStatus);
        if ("available".equals(targetStatus)) {
            suggestedGuest = tableReadService.findSuggestedWaitlistGuest(existing.capacity());
            message = suggestedGuest == null ? "Table marked available." : "Table marked available. Suggested next waitlist guest: " + suggestedGuest + ".";
        } else if ("out_of_service".equals(targetStatus)) {
            message = "Table marked out of service.";
        }
        auditService.record(actor.userId(), action, "DiningTable", tableId.toString(), correlationId, reason, false,
                requestMetadata.remoteIp(), Map.of("status", existing.status()),
                Map.of("status", targetStatus, "suggestedWaitlistGuest", suggestedGuest == null ? "" : suggestedGuest));
        return new SA.irms.reservation.application.view.ReservationViews.TableActionResult(tableId, targetStatus, message, suggestedGuest);
    }

    private String normalizeTableStatus(String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new ConflictException("A target table status is required.");
        }
        String normalized = targetStatus.trim().toLowerCase();
        if (!List.of("cleaning", "available", "out_of_service").contains(normalized)) {
            throw new ConflictException("Unsupported table status transition.");
        }
        return normalized;
    }
}
