package SA.irms.identity.application.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class IdentityCommands {
    private IdentityCommands() {
    }

    public record CreateShiftRequest(
            UUID userId,
            LocalDate shiftDate,
            LocalTime startAt,
            LocalTime endAt,
            String position,
            String zone
    ) {
    }

    public record UpdateRolesRequest(UUID userId, List<UUID> roleIds) {
    }

    public record UpdateSettingsRequest(
            int waitlistHoldMinutes,
            int reservationGraceMinutes,
            int kitchenRushThresholdMinutes,
            int kitchenLateThresholdMinutes,
            int refundWindowHours
    ) {
    }
}
