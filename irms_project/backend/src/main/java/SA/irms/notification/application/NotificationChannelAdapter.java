package SA.irms.notification.application;

public interface NotificationChannelAdapter {
    String channel();

    void validate(NotificationService.Command command);
}
