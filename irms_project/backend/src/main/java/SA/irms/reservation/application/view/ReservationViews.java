package SA.irms.reservation.application.view;

import java.util.List;
import java.util.UUID;

public final class ReservationViews {
    private ReservationViews() {}

    public record ReservationOverview(List<TableView> tables, List<ReservationView> reservations, List<WaitlistView> waitlist) {}
    public record ReservationRecommendation(String date, String time, int party, List<RecommendedTable> candidateTables,
                                            boolean waitlistRecommended, int estimatedWaitMinutes) {}
    public record TableView(UUID id, int number, int capacity, String status, String notes, UUID sessionId, String checkinTime, String reservationTime) {}
    public record ReservationView(UUID id, String guest, String phone, String email, int party, String date, String time, String status, Integer table, String notes) {}
    public record WaitlistView(UUID id, String name, String phone, String email, int party, String estimatedWait, String status,
                               String holdExpiresAt, String notes, int tablesAvailableNow, Integer suggestedTable) {}
    public record RecommendedTable(UUID id, int number, int capacity) {}
    public record TableActionResult(UUID tableId, String status, String message, String suggestedWaitlistGuest) {}
}
