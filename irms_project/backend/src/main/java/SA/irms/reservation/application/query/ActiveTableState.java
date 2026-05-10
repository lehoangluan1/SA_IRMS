package SA.irms.reservation.application.query;

import java.util.UUID;

public record ActiveTableState(UUID sessionId, boolean hasOutstandingBalance) {
    public boolean hasActiveSession() {
        return sessionId != null;
    }
}
