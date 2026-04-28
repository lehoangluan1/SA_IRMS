package SA.irms.reservation.application.query;

import java.util.UUID;

public record TableCandidate(UUID tableId, int number, int capacity, String status) {
}
