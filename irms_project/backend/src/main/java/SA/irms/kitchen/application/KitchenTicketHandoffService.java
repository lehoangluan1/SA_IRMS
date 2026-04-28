package SA.irms.kitchen.application;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.kitchen.application.port.out.KitchenTicketAutoServePort;
import SA.irms.kitchen.application.port.out.KitchenTicketHandoffCommandPort;
import SA.irms.kitchen.application.port.out.KitchenTicketItemRepositoryPort;
import SA.irms.kitchen.application.port.out.KitchenTicketQueryRepository;
import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import SA.irms.kitchen.application.support.KitchenTimingPolicy;
import SA.irms.common.context.RequestMetadata;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenTicketHandoffService implements KitchenTicketAutoServePort {
    private final KitchenTicketHandoffCommandPort handoffCommandPort;
    private final AuditRecorder auditService;
    private final KitchenTicketItemRepositoryPort ticketItemRepository;
    private final KitchenTicketEventNotifier eventNotifier;
    private final KitchenTicketStateCoordinatorPort ticketStateCoordinator;
    private final KitchenTicketQueryRepository kitchenTicketReadService;
    private final KitchenTimingPolicy timingPolicy;
    private final Clock clock;

    public KitchenTicketHandoffService(
            KitchenTicketHandoffCommandPort handoffCommandPort,
            AuditRecorder auditService,
            KitchenTicketItemRepositoryPort ticketItemRepository,
            KitchenTicketEventNotifier eventNotifier,
            KitchenTicketStateCoordinatorPort ticketStateCoordinator,
            KitchenTicketQueryRepository kitchenTicketReadService,
            KitchenTimingPolicy timingPolicy,
            Clock clock
    ) {
        this.handoffCommandPort = handoffCommandPort;
        this.auditService = auditService;
        this.ticketItemRepository = ticketItemRepository;
        this.eventNotifier = eventNotifier;
        this.ticketStateCoordinator = ticketStateCoordinator;
        this.kitchenTicketReadService = kitchenTicketReadService;
        this.timingPolicy = timingPolicy;
        this.clock = clock;
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView serveTicket(UUID ticketId, AuthenticatedUser actor) {
        if (ticketItemRepository.countItemsNotReadyForServing(ticketId) > 0) {
            throw new ConflictException("Only ready dishes can be served.");
        }
        serveReadyItems(ticketId, actor);
        return kitchenTicketReadService.findTicket(ticketId);
    }

    @Override
    @Transactional
    public void autoServeReadyTicket(UUID ticketId, AuthenticatedUser actor) {
        if (ticketItemRepository.countItemsNotReadyForServing(ticketId) > 0) {
            return;
        }
        serveReadyItems(ticketId, actor);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView returnTicket(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        if (reason == null || reason.isBlank()) {
            throw new ConflictException("A return reason is required.");
        }
        UUID handoffId = handoffCommandPort.findHandoffId(ticketId)
                .orElseThrow(() -> new ConflictException("Only handed-off tickets can be returned."));
        List<KitchenTicketItemContext> returnedItems = ticketItemRepository.loadTicketItemContexts(ticketId, List.of("served"));
        if (returnedItems.isEmpty()) {
            throw new ConflictException("Only served dishes can be returned.");
        }
        handoffCommandPort.markHandoffReturned(handoffId, reason);
        handoffCommandPort.markServedItemsReady(ticketId, reason);
        handoffCommandPort.markTicketReturned(ticketId, Instant.now(clock).plusSeconds(timingPolicy.randomReadyToServedSeconds()));
        ticketStateCoordinator.reconcileTicket(ticketId);
        returnedItems.forEach(item -> {
            eventNotifier.publishDishStatus(item, handoffId, "returned", Map.of("ticketId", ticketId.toString(), "reason", reason));
            eventNotifier.queueReadyNotification(item);
        });
        auditService.record(actor.userId(), "kitchen.ticket.returned", "KitchenTicket", ticketId.toString(), correlationId,
                reason, false, request.remoteIp(), Map.of("status", "served"), Map.of("status", "ready", "reason", reason));
        return kitchenTicketReadService.findTicket(ticketId);
    }

    private void serveReadyItems(UUID ticketId, AuthenticatedUser actor) {
        List<KitchenTicketItemContext> servedItems = ticketItemRepository.loadTicketItemContexts(ticketId, List.of("ready"));
        if (servedItems.isEmpty()) {
            return;
        }
        UUID handoffId = handoffCommandPort.findHandoffId(ticketId)
                .orElseGet(() -> handoffCommandPort.createHandoff(ticketId, actor.userId()));
        handoffCommandPort.markReadyItemsServed(ticketId);
        handoffCommandPort.markTicketServed(ticketId);
        ticketStateCoordinator.reconcileTicket(ticketId);
        servedItems.forEach(item -> eventNotifier.publishDishStatus(item, handoffId, "served", Map.of("ticketId", ticketId.toString())));
    }
}
