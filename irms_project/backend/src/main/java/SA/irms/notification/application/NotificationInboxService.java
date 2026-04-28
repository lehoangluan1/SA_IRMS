package SA.irms.notification.application;

import SA.irms.notification.application.port.out.NotificationInboxRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;

@Service
public class NotificationInboxService {
    private final NotificationInboxRepository repository;

    public NotificationInboxService(NotificationInboxRepository repository) {
        this.repository = repository;
    }

    public List<SA.irms.notification.application.view.NotificationViews.NotificationView> loadInbox(AuthenticatedUser user) {
        return repository.loadInbox(user);
    }

    @Transactional
    public SA.irms.notification.application.view.NotificationViews.NotificationView markRead(UUID messageId, AuthenticatedUser user) {
        return repository.markRead(messageId, user);
    }

}
