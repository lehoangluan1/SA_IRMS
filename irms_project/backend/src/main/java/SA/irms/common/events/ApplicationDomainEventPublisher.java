package SA.irms.common.events;

public interface ApplicationDomainEventPublisher {
    void publish(Object event);
}
