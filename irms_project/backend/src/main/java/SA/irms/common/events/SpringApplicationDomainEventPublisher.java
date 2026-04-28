package SA.irms.common.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationDomainEventPublisher implements ApplicationDomainEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringApplicationDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
