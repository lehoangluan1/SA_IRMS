package SA.irms.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import SA.irms.common.config.AppProperties;
import SA.irms.common.outbox.OutboxEventPublisher;
import SA.irms.common.events.ServiceEventTypes;

@Component
public class SessionTouchRecorder {
    private final OutboxEventPublisher outboxEventPublisher;
    private final AppProperties properties;
    private final Clock clock;
    private final Map<UUID, Instant> lastPublishedBySession = new ConcurrentHashMap<>();

    public SessionTouchRecorder(OutboxEventPublisher outboxEventPublisher, AppProperties properties, Clock clock) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    public void recordActivity(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        Instant now = Instant.now(clock);
        Duration debounce = Duration.ofSeconds(Math.max(1, properties.security().sessionTouchDebounceSeconds()));
        Instant previous = lastPublishedBySession.get(sessionId);
        if (previous != null && previous.plus(debounce).isAfter(now)) {
            return;
        }
        lastPublishedBySession.put(sessionId, now);
        outboxEventPublisher.publish(
                ServiceEventTypes.SESSION_TOUCH_REQUESTED,
                "UserSession",
                sessionId.toString(),
                Map.of("sessionId", sessionId.toString(), "lastActivityAt", now.toString()),
                sessionId.toString()
        );
    }
}
