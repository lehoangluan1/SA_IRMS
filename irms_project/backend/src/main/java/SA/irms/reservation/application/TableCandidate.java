package SA.irms.reservation.application;

import java.util.UUID;

record TableCandidate(UUID tableId, int number, int capacity, String status) {
}
