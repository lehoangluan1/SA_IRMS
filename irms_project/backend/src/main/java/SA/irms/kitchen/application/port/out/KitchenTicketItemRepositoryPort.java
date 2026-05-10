package SA.irms.kitchen.application.port.out;

import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import java.util.List;
import java.util.UUID;

public interface KitchenTicketItemRepositoryPort {
    List<KitchenTicketItemContext> loadTicketItemContexts(UUID ticketId, List<String> statuses);
    KitchenTicketItemContext loadForUpdate(UUID ticketItemId);
    KitchenTicketItemContext loadForAutomationUpdate(UUID ticketItemId);
    boolean everyTicketItemReady(UUID ticketId);
    long countItemsNotReadyForServing(UUID ticketId);
}
