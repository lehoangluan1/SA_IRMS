package SA.irms.inventory.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class InventoryRelativeTimeFormatter {
    private final Clock clock;

    InventoryRelativeTimeFormatter(Clock clock) {
        this.clock = clock;
    }

    String format(Instant instant) {
        long minutes = Math.max(0, Duration.between(instant, Instant.now(clock)).toMinutes());
        if (minutes < 60) {
            return minutes + " min ago";
        }
        if (minutes < 1440) {
            return (minutes / 60) + " hrs ago";
        }
        return (minutes / 1440) + " days ago";
    }
}
