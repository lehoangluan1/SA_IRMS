package SA.irms.reservation.application.port.out;

import java.util.UUID;

public interface ReservationTableCommandPort {
    UUID createTable(UUID branchId, int number, int capacity, String sectionLabel);

    void updateTable(UUID tableId, int number, int capacity, String sectionLabel);

    long countActiveSessions(UUID tableId);

    int deleteTable(UUID tableId);

    void closeActiveSession(UUID sessionId);

    void releaseAssignmentsByTable(UUID tableId, String reason);

    void updateTableStatus(UUID tableId, String targetStatus);
}
