package SA.irms.kitchen.domain;

import java.time.Instant;

public class KitchenPriorityPolicy {
    public String nextManualPriority(String currentPriority) {
        return switch (currentPriority) {
            case "normal" -> "rush";
            case "rush" -> "expedite";
            default -> "normal";
        };
    }

    public int score(String priorityLabel) {
        return switch (priorityLabel) {
            case "rush" -> 8;
            case "expedite" -> 10;
            default -> 5;
        };
    }

    public String automaticEscalation(String currentPriority, Instant targetServiceAt, Instant rushCutoff, Instant expediteCutoff) {
        if ("normal".equals(currentPriority) && !targetServiceAt.isAfter(expediteCutoff)) {
            return "expedite";
        }
        if ("rush".equals(currentPriority) && !targetServiceAt.isAfter(expediteCutoff)) {
            return "expedite";
        }
        if ("normal".equals(currentPriority) && !targetServiceAt.isAfter(rushCutoff)) {
            return "rush";
        }
        return null;
    }
}
