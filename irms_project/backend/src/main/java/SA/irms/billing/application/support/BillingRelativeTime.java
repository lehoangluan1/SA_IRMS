package SA.irms.billing.application.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class BillingRelativeTime {
    private final Clock clock;

    public BillingRelativeTime(Clock clock) {
        this.clock = clock;
    }

    public String format(Instant instant) {
        long minutes = Math.max(0, Duration.between(instant, Instant.now(clock)).toMinutes());
        if (minutes < 60) {
            return minutes + " min ago";
        }
        return (minutes / 60) + " hr ago";
    }
}
