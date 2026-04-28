package SA.irms.notification.application.port.out;

import SA.irms.common.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public interface NotificationInboxRepository {
    List<SA.irms.notification.application.view.NotificationViews.NotificationView> loadInbox(AuthenticatedUser user);

    SA.irms.notification.application.view.NotificationViews.NotificationView markRead(UUID messageId, AuthenticatedUser user);
}
