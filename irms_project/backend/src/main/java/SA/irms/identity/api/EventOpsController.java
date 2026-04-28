package SA.irms.identity.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.QueuedOperationResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.identity.application.EventOperationsService;
import SA.irms.identity.application.view.IdentityViews;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/audit/events")
public class EventOpsController {
    private final PermissionGuard permissionGuard;
    private final EventOperationsService eventOperationsService;

    public EventOpsController(PermissionGuard permissionGuard, EventOperationsService eventOperationsService) {
        this.permissionGuard = permissionGuard;
        this.eventOperationsService = eventOperationsService;
    }

    @GetMapping("/failed-outbox")
    public ApiEnvelope<List<IdentityViews.FailedOutboxEventView>> failedOutbox(HttpServletRequest request) {
        permissionGuard.require("audit.view");
        return ApiEnvelope.of(eventOperationsService.failedOutbox(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/dead-letter")
    public ApiEnvelope<List<IdentityViews.DeadLetterEventView>> deadLetters(HttpServletRequest request) {
        permissionGuard.require("audit.view");
        return ApiEnvelope.of(eventOperationsService.deadLetters(), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/outbox/{eventId}/replay")
    public ApiEnvelope<QueuedOperationResponse> replayOutbox(
            @PathVariable UUID eventId,
            @RequestBody(required = false) ReplayBody body,
            HttpServletRequest request
    ) {
        permissionGuard.require("audit.view");
        eventOperationsService.replayOutbox(eventId, body == null ? "Manual outbox replay" : body.reason());
        return ApiEnvelope.of(QueuedOperationResponse.queued(eventId), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/consumer/{eventId}/{consumerName}/replay")
    public ApiEnvelope<QueuedOperationResponse> replayConsumer(
            @PathVariable UUID eventId,
            @PathVariable String consumerName,
            @RequestBody(required = false) ReplayBody body,
            HttpServletRequest request
    ) {
        permissionGuard.require("audit.view");
        eventOperationsService.replayConsumer(eventId, consumerName, body == null ? "Manual consumer replay" : body.reason());
        return ApiEnvelope.of(QueuedOperationResponse.queued(eventId, consumerName), RequestContext.getCorrelationId(request));
    }

    public record ReplayBody(String reason) {
    }
}
