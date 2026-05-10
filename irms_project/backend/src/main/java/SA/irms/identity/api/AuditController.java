package SA.irms.identity.api;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.identity.audit.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService auditService;
    private final PermissionGuard permissionGuard;
    private final ObjectMapper objectMapper;

    public AuditController(AuditService auditService, PermissionGuard permissionGuard, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.permissionGuard = permissionGuard;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/logs")
    public ApiEnvelope<?> logs(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "actorRole", required = false) String actorRole,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("audit.view");
        var result = auditService.search(search, action, actorRole, startDate, endDate, limit, actor);
        auditService.recordAccess(
                actor,
                "audit.log.viewed",
                RequestContext.getCorrelationId(request),
                request.getRemoteAddr(),
                Map.of(
                        "search", search == null ? "" : search,
                        "action", action == null ? "" : action,
                        "actorRole", actorRole == null ? "" : actorRole,
                        "startDate", startDate == null ? "" : startDate.toString(),
                        "endDate", endDate == null ? "" : endDate.toString()
                )
        );
        return ApiEnvelope.of(result, RequestContext.getCorrelationId(request));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "actorRole", required = false) String actorRole,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "format", defaultValue = "csv") String format,
            HttpServletRequest request
    ) throws JsonProcessingException {
        var actor = permissionGuard.require("audit.view");
        var result = auditService.search(search, action, actorRole, startDate, endDate, 500, actor);
        auditService.recordAccess(
                actor,
                "audit.log.exported",
                RequestContext.getCorrelationId(request),
                request.getRemoteAddr(),
                Map.of(
                        "search", search == null ? "" : search,
                        "action", action == null ? "" : action,
                        "actorRole", actorRole == null ? "" : actorRole,
                        "startDate", startDate == null ? "" : startDate.toString(),
                        "endDate", endDate == null ? "" : endDate.toString(),
                        "format", format
                )
        );
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new AuditExportResponse(result)));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(auditService.exportCsv(result));
    }

    @PatchMapping("/logs/{auditLogId}/follow-up")
    public ApiEnvelope<AuditFollowUpResponse> followUp(
            @PathVariable UUID auditLogId,
            @RequestBody FollowUpRequest followUpRequest,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("audit.view");
        auditService.setFollowUp(auditLogId, followUpRequest.followUp());
        auditService.record(
                actor.userId(),
                "audit.log.follow_up.updated",
                "AuditLog",
                auditLogId.toString(),
                RequestContext.getCorrelationId(request),
                "Follow-up flag updated from the audit screen.",
                false,
                request.getRemoteAddr(),
                Map.of(),
                Map.of("followUp", followUpRequest.followUp())
        );
        return ApiEnvelope.of(new AuditFollowUpResponse(auditLogId, followUpRequest.followUp()),
                RequestContext.getCorrelationId(request));
    }

    public record FollowUpRequest(boolean followUp) {
    }

    public record AuditFollowUpResponse(UUID auditLogId, boolean followUp) {
    }

    public record AuditExportResponse(Object data) {
    }
}
