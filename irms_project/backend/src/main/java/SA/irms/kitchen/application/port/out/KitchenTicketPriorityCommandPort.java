package SA.irms.kitchen.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface KitchenTicketPriorityCommandPort {
    List<AutoPriorityCandidate> loadAutoPriorityCandidates();

    void updatePriority(UUID ticketId, String priorityLabel, int priorityScore, boolean keepExistingExpediteTimestamp);

    record AutoPriorityCandidate(UUID ticketId, String priorityLabel, Instant targetServiceAt) {
    }
}
