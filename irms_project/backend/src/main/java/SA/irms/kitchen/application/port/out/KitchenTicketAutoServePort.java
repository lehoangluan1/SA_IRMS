package SA.irms.kitchen.application.port.out;

import java.util.UUID;
import SA.irms.common.security.AuthenticatedUser;

public interface KitchenTicketAutoServePort {
    void autoServeReadyTicket(UUID ticketId, AuthenticatedUser actor);
}
