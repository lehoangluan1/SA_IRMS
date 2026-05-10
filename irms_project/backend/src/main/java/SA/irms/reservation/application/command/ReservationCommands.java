package SA.irms.reservation.application.command;

import java.util.UUID;

public final class ReservationCommands {
    private ReservationCommands() {}

    public record ReservationUpsert(String guest, String phone, String email, int party, String date, String time, String notes, UUID tableId, boolean fallbackToWaitlist) {}
    public record ReservationPatch(String guest, int party, String notes) {}
    public record TableUpsert(int number, int capacity, String notes) {}
    public record TableStatusUpdate(String targetStatus, String reason) {}
    public record CheckInRequest(Boolean approveRecovery, Integer actualPartySize, UUID replacementTableId) {}
    public record WaitlistUpsert(String name, String phone, String email, String notes, int party) {}
}
