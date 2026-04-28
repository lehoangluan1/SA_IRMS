package SA.irms.kitchen.application;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.kitchen.application.port.out.KitchenTicketItemRepositoryPort;
import SA.irms.kitchen.application.port.out.KitchenTicketQueryRepository;
import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import SA.irms.kitchen.domain.KitchenStatusPolicy;
import SA.irms.common.context.RequestMetadata;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenService {
    private final AuditRecorder auditService;
    private final KitchenTicketStateCoordinatorPort ticketStateCoordinator;
    private final KitchenTicketQueryRepository kitchenTicketReadService;
    private final KitchenTicketItemRepositoryPort ticketItemRepository;
    private final KitchenTicketItemWorkflowService itemWorkflowService;
    private final KitchenTicketCancellationService cancellationService;
    private final KitchenTicketHandoffService handoffService;
    private final KitchenPriorityService priorityService;
    private final KitchenStatusPolicy kitchenStatusPolicy;

    public KitchenService(
            AuditRecorder auditService,
            KitchenTicketStateCoordinatorPort ticketStateCoordinator,
            KitchenTicketQueryRepository kitchenTicketReadService,
            KitchenTicketItemRepositoryPort ticketItemRepository,
            KitchenTicketItemWorkflowService itemWorkflowService,
            KitchenTicketCancellationService cancellationService,
            KitchenTicketHandoffService handoffService,
            KitchenPriorityService priorityService,
            KitchenStatusPolicy kitchenStatusPolicy
    ) {
        this.auditService = auditService;
        this.ticketStateCoordinator = ticketStateCoordinator;
        this.kitchenTicketReadService = kitchenTicketReadService;
        this.ticketItemRepository = ticketItemRepository;
        this.itemWorkflowService = itemWorkflowService;
        this.cancellationService = cancellationService;
        this.handoffService = handoffService;
        this.priorityService = priorityService;
        this.kitchenStatusPolicy = kitchenStatusPolicy;
    }

    public SA.irms.kitchen.application.view.KitchenViews.KitchenOverview load(String stationFilter) {
        return kitchenTicketReadService.load(stationFilter);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView updateTicketStatus(UUID ticketId, String targetStatus, AuthenticatedUser actor, String correlationId) {
        String normalizedTargetStatus = kitchenStatusPolicy.normalize(targetStatus);
        SA.irms.kitchen.application.view.KitchenViews.TicketView currentTicket = kitchenTicketReadService.findTicket(ticketId);
        if (currentTicket.status().equals(normalizedTargetStatus)) {
            return currentTicket;
        }
        kitchenStatusPolicy.ensureValidTicketTransition(currentTicket.status(), normalizedTargetStatus, ticketItemRepository.everyTicketItemReady(ticketId));
        if ("cooking".equals(normalizedTargetStatus)) {
            List<UUID> queuedItemIds = ticketItemRepository.loadTicketItemContexts(ticketId, List.of("queued")).stream()
                    .map(KitchenTicketItemContext::ticketItemId)
                    .toList();
            for (UUID queuedItemId : queuedItemIds) {
                itemWorkflowService.moveItemToCooking(ticketItemRepository.loadForUpdate(queuedItemId), actor.userId(), correlationId);
            }
        }
        ticketStateCoordinator.reconcileTicket(ticketId);
        return kitchenTicketReadService.findTicket(ticketId);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView updateItemStatus(UUID ticketItemId, String targetStatus, String reason, AuthenticatedUser actor,
                                       String correlationId, RequestMetadata request) {
        String normalizedTargetStatus = kitchenStatusPolicy.normalize(targetStatus);
        KitchenTicketItemContext itemContext = ticketItemRepository.loadForUpdate(ticketItemId);
        if (itemContext.status().equals(normalizedTargetStatus)) {
            return kitchenTicketReadService.findTicket(itemContext.ticketId());
        }
        kitchenStatusPolicy.ensureValidItemTransition(itemContext.status(), normalizedTargetStatus);
        if ("blocked".equals(normalizedTargetStatus) && (reason == null || reason.isBlank())) {
            throw new ConflictException("A blocked reason is required.");
        }
        if ("blocked".equals(normalizedTargetStatus)
                && kitchenStatusPolicy.hasCookingStarted(itemContext.status(), itemContext.cookingStartedAt(), itemContext.inventoryDeductedAt())
                && !cancellationService.canOverrideKitchenCancellation(actor)) {
            throw new ConflictException("Manager approval is required once kitchen work has started.");
        }
        applyItemTransition(itemContext, normalizedTargetStatus, reason, actor.userId(), correlationId);
        ticketStateCoordinator.reconcileTicket(itemContext.ticketId());
        if ("blocked".equals(normalizedTargetStatus)) {
            auditService.record(actor.userId(), "kitchen.item.cancelled", "KitchenTicketItem", ticketItemId.toString(), correlationId,
                    reason, false, request.remoteIp(), Map.of("status", itemContext.status()), Map.of("status", "blocked", "reason", reason));
        }
        return kitchenTicketReadService.findTicket(itemContext.ticketId());
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView togglePriority(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        return priorityService.togglePriority(ticketId, reason, actor, correlationId, request);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView cancelTicket(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        return cancellationService.cancelTicket(ticketId, reason, actor, correlationId, request);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView serveTicket(UUID ticketId, AuthenticatedUser actor, String correlationId) {
        return handoffService.serveTicket(ticketId, actor);
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView returnTicket(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        return handoffService.returnTicket(ticketId, reason, actor, correlationId, request);
    }

    private void applyItemTransition(KitchenTicketItemContext itemContext, String normalizedTargetStatus, String reason, UUID actorUserId, String correlationId) {
        if ("cooking".equals(normalizedTargetStatus)) {
            itemWorkflowService.moveItemToCooking(itemContext, actorUserId, correlationId);
        } else if ("ready".equals(normalizedTargetStatus)) {
            itemWorkflowService.markItemReady(itemContext);
        } else if ("blocked".equals(normalizedTargetStatus)) {
            itemWorkflowService.blockItem(itemContext, reason);
        }
    }
}
