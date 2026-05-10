package SA.irms.kitchen.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface KitchenTicketHandoffCommandPort {
    Optional<UUID> findHandoffId(UUID ticketId);

    UUID createHandoff(UUID ticketId, UUID serverId);

    void markReadyItemsServed(UUID ticketId);

    void markTicketServed(UUID ticketId);

    void markHandoffReturned(UUID handoffId, String reason);

    void markServedItemsReady(UUID ticketId, String reason);

    void markTicketReturned(UUID ticketId, Instant nextActionAt);
}
