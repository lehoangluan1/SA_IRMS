package SA.irms.reservation.application.port.out;

import SA.irms.reservation.application.query.WaitlistRow;
import java.util.UUID;

public interface WaitlistQueryRepository {
    SA.irms.reservation.application.view.ReservationViews.WaitlistView findWaitlistEntry(UUID waitlistEntryId);
    WaitlistRow loadWaitlistRow(UUID waitlistEntryId);
    int estimateQuotedWaitMinutes();
    UUID requireAvailableTableIdForWaitlist(int partySize);
}
