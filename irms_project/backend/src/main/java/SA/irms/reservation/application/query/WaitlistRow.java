package SA.irms.reservation.application.query;

import java.time.Instant;
import java.util.UUID;

public record WaitlistRow(UUID waitlistEntryId, int partySize, String status, int priority, Instant holdExpiresAt) {
}
