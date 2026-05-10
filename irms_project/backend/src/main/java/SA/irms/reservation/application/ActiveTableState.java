package SA.irms.reservation.application;

import java.util.UUID;

record ActiveTableState(UUID sessionId, boolean hasOutstandingBalance) {
    boolean hasActiveSession() {
        return sessionId != null;
    }
}
