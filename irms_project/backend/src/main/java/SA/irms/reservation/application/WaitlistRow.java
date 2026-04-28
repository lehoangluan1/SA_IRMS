package SA.irms.reservation.application;

import java.time.Instant;
import java.util.UUID;

record WaitlistRow(UUID waitlistEntryId, int partySize, String status, int priority, Instant holdExpiresAt) {
}
