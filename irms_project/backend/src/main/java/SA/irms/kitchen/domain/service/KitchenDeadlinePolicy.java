package SA.irms.kitchen.domain.service;

import java.time.Clock;
import java.time.Instant;

public class KitchenDeadlinePolicy {
    private final Clock clock;

    public KitchenDeadlinePolicy(Clock clock) {
        this.clock = clock;
    }

    public Instant calculateDeadline(int preparationMinutes, int itemCount, int priority, boolean peakHour) {
        int safePreparationMinutes = Math.max(1, preparationMinutes);
        int quantityModifier = Math.max(0, itemCount - 1) * 2;
        int priorityReduction = Math.max(0, Math.min(priority, 10) - 5);
        double peakModifier = peakHour ? 1.25 : 1.0;
        long minutes = Math.max(1, Math.round((safePreparationMinutes + quantityModifier - priorityReduction) * peakModifier));
        return Instant.now(clock).plusSeconds(minutes * 60L);
    }

    public boolean isDelayed(Instant deadline) {
        return deadline != null && Instant.now(clock).isAfter(deadline);
    }
}
