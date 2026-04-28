package SA.irms.kitchen.domain;

import java.time.Instant;
import java.util.List;

import SA.irms.common.error.ConflictException;

public class KitchenStatusPolicy {
    public String normalize(String status) {
        if (status == null || status.isBlank()) {
            return "queued";
        }
        return switch (status.trim().toLowerCase()) {
            case "new", "queued" -> "queued";
            case "started", "cooking" -> "cooking";
            case "ready" -> "ready";
            case "blocked" -> "blocked";
            case "hold_for_service" -> "hold_for_service";
            case "served" -> "served";
            default -> throw new ConflictException("Unsupported kitchen item status.");
        };
    }

    public void ensureValidTicketTransition(String currentStatus, String targetStatus, boolean everyTicketItemReady) {
        boolean valid = switch (targetStatus) {
            case "cooking" -> "queued".equals(currentStatus);
            case "ready" -> "cooking".equals(currentStatus) && everyTicketItemReady;
            default -> false;
        };
        if (!valid) {
            throw new ConflictException("The requested kitchen ticket transition is not allowed for the current ticket state.");
        }
    }

    public void ensureValidItemTransition(String currentStatus, String targetStatus) {
        boolean valid = switch (targetStatus) {
            case "cooking" -> "queued".equals(currentStatus);
            case "ready" -> "cooking".equals(currentStatus);
            case "blocked" -> List.of("queued", "cooking").contains(currentStatus);
            default -> false;
        };
        if (!valid) {
            throw new ConflictException("The requested kitchen item transition is not allowed for the current item state.");
        }
    }

    public String deriveTicketStatus(List<String> itemStatuses) {
        if (itemStatuses == null || itemStatuses.isEmpty()) {
            return "queued";
        }
        if (itemStatuses.stream().allMatch("blocked"::equals)) {
            return "blocked";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("served", "blocked").contains(status))) {
            return "served";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("ready", "blocked").contains(status))) {
            return "ready";
        }
        if (itemStatuses.stream().anyMatch("cooking"::equals)) {
            return "cooking";
        }
        if (itemStatuses.stream().allMatch(status -> List.of("hold_for_service", "blocked").contains(status))) {
            return "hold_for_service";
        }
        return "queued";
    }

    public boolean hasCookingStarted(String status, Instant cookingStartedAt, Instant inventoryDeductedAt) {
        return cookingStartedAt != null
                || inventoryDeductedAt != null
                || List.of("cooking", "ready", "served").contains(status);
    }
}
