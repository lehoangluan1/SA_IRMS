package SA.irms.notification.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.CurrentUser;
import SA.irms.common.web.RequestContext;
import SA.irms.notification.application.NotificationInboxService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationInboxService notificationInboxService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationInboxService notificationInboxService, CurrentUser currentUser) {
        this.notificationInboxService = notificationInboxService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiEnvelope<List<SA.irms.notification.application.view.NotificationViews.NotificationView>> list(HttpServletRequest request) {
        return ApiEnvelope.of(notificationInboxService.loadInbox(currentUser.require()), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{messageId}/read")
    public ApiEnvelope<SA.irms.notification.application.view.NotificationViews.NotificationView> markRead(
            @PathVariable UUID messageId,
            HttpServletRequest request
    ) {
        return ApiEnvelope.of(notificationInboxService.markRead(messageId, currentUser.require()), RequestContext.getCorrelationId(request));
    }
}
