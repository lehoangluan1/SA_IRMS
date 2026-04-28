package SA.irms.ordering.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest;
import SA.irms.ordering.application.view.OrderViews.OrdersOverview;
import SA.irms.common.context.RequestMetadata;

@Service
class OrderCreationService {
    private final OrderCreationValidator validator;
    private final OrderPersistenceCoordinator persistenceCoordinator;
    private final OrderCreationAuditPublisher auditPublisher;
    private final OrderSnapshotService orderSnapshotService;
    private final OrdersReadService ordersReadService;
    private final Clock clock;

    OrderCreationService(
            OrderCreationValidator validator,
            OrderPersistenceCoordinator persistenceCoordinator,
            OrderCreationAuditPublisher auditPublisher,
            OrderSnapshotService orderSnapshotService,
            OrdersReadService ordersReadService,
            Clock clock
    ) {
        this.validator = validator;
        this.persistenceCoordinator = persistenceCoordinator;
        this.auditPublisher = auditPublisher;
        this.orderSnapshotService = orderSnapshotService;
        this.ordersReadService = ordersReadService;
        this.clock = clock;
    }

    @Transactional
    OrdersOverview createOrder(CreateOrderRequest request, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        validator.validateCreateOrderRequest(request);
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        OrderCreationResult created = persistenceCoordinator.createOrder(orderId, request, actor.userId(), correlationId, now);
        if (request.draft()) {
            auditPublisher.auditDraftOrder(
                    actor,
                    orderId,
                    correlationId,
                    httpServletRequest,
                    request.sessionId(),
                    created.kitchenItems().size(),
                    created.subtotal()
            );
        } else {
            orderSnapshotService.upsertOrderSnapshot(orderId, created.subtotal(), now);
            auditPublisher.publishOrderConfirmed(orderId, request, created, correlationId);
            auditPublisher.auditConfirmedOrder(
                    actor,
                    orderId,
                    correlationId,
                    httpServletRequest,
                    request.sessionId(),
                    created.kitchenItems().size(),
                    created.subtotal()
            );
        }
        return ordersReadService.load(request.sessionId());
    }
}
