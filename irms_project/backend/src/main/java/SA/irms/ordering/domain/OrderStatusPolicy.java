package SA.irms.ordering.domain;

import java.util.List;

public class OrderStatusPolicy {
    public String deriveOrderStatus(List<String> itemStatuses) {
        if (itemStatuses == null || itemStatuses.isEmpty()) {
            return "confirmed";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("cancelled", "blocked").contains(status))) {
            return "cancelled";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("served", "cancelled", "blocked").contains(status))) {
            return "served";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("ready", "served", "cancelled", "blocked").contains(status))) {
            return "ready";
        }
        if (itemStatuses.stream().anyMatch("cooking"::equals)) {
            return "in_progress";
        }
        return "confirmed";
    }
}
