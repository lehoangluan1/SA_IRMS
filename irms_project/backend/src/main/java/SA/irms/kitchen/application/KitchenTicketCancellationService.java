package SA.irms.kitchen.application;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.kitchen.application.port.out.KitchenTicketCancellationCommandPort;
import SA.irms.kitchen.application.port.out.KitchenTicketQueryRepository;
import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import SA.irms.common.context.RequestMetadata;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenTicketCancellationService {
    private final KitchenTicketCancellationCommandPort cancellationCommandPort;
    private final AuditRecorder auditService;
    private final KitchenTicketQueryRepository kitchenTicketReadService;
    private final KitchenTicketStateCoordinatorPort ticketStateCoordinator;

    public KitchenTicketCancellationService(
            KitchenTicketCancellationCommandPort cancellationCommandPort,
            AuditRecorder auditService,
            KitchenTicketQueryRepository kitchenTicketReadService,
            KitchenTicketStateCoordinatorPort ticketStateCoordinator
    ) {
        this.cancellationCommandPort = cancellationCommandPort;
        this.auditService = auditService;
        this.kitchenTicketReadService = kitchenTicketReadService;
        this.ticketStateCoordinator = ticketStateCoordinator;
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView cancelTicket(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        SA.irms.kitchen.application.view.KitchenViews.TicketView ticket = kitchenTicketReadService.findTicket(ticketId);
        boolean kitchenWorkStarted = ticket.items().stream().anyMatch(item -> List.of("cooking", "ready", "served").contains(item.status()));
        if (kitchenWorkStarted && !canOverrideKitchenCancellation(actor)) {
            throw new ConflictException("Manager approval is required once kitchen work has started.");
        }
        cancellationCommandPort.blockOpenItems(ticketId, reason);
        cancellationCommandPort.blockTicket(ticketId, reason);
        ticketStateCoordinator.reconcileTicket(ticketId);
        auditService.record(actor.userId(), "kitchen.ticket.cancelled", "KitchenTicket", ticketId.toString(), correlationId,
                reason, false, request.remoteIp(), Map.of("status", "active"), Map.of("status", "blocked"));
        return kitchenTicketReadService.findTicket(ticketId);
    }

    public boolean canOverrideKitchenCancellation(AuthenticatedUser actor) {
        return actor.hasRole("manager") || actor.hasRole("admin");
    }
}
