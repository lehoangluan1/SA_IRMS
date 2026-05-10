package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.ordering.application.port.out.OrderQueryRepository;

@Service
class OrdersReadService {
    private final OrderQueryRepository queryRepository;

    OrdersReadService(OrderQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    SA.irms.ordering.application.view.OrderViews.OrdersOverview load(UUID sessionId) {
        List<SA.irms.ordering.application.view.OrderViews.TableSessionView> sessions = queryRepository.loadTableSessions();
        UUID resolvedSessionId = sessionId != null ? sessionId : sessions.stream().findFirst()
                .map(SA.irms.ordering.application.view.OrderViews.TableSessionView::sessionId)
                .orElse(null);
        List<SA.irms.ordering.application.view.OrderViews.OrderedItemView> orderedItems = resolvedSessionId == null ? List.of() : queryRepository.loadOrderedItems(resolvedSessionId);
        return new SA.irms.ordering.application.view.OrderViews.OrdersOverview(sessions, queryRepository.loadMenuItemsForOrdering(), queryRepository.loadMenuCombos(), orderedItems, resolvedSessionId);
    }
}
