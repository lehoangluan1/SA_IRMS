package SA.irms.kitchen.domain.service;

import java.util.List;
import java.util.Map;

import SA.irms.common.error.ConflictException;
import SA.irms.kitchen.domain.KitchenTicketState;

public class KitchenTicketTransitionPolicy {
    private static final Map<KitchenTicketState, List<KitchenTicketState>> VALID_TRANSITIONS = Map.of(
            KitchenTicketState.QUEUED, List.of(KitchenTicketState.STARTED, KitchenTicketState.COOKING, KitchenTicketState.CANCELLED),
            KitchenTicketState.STARTED, List.of(KitchenTicketState.COOKING, KitchenTicketState.CANCELLED),
            KitchenTicketState.COOKING, List.of(KitchenTicketState.READY_TO_SERVE, KitchenTicketState.RETURNED, KitchenTicketState.CANCELLED),
            KitchenTicketState.READY_TO_SERVE, List.of(KitchenTicketState.SERVED, KitchenTicketState.RETURNED, KitchenTicketState.CANCELLED),
            KitchenTicketState.RETURNED, List.of(KitchenTicketState.COOKING, KitchenTicketState.CANCELLED),
            KitchenTicketState.SERVED, List.of(),
            KitchenTicketState.CANCELLED, List.of()
    );

    public void ensureValidTransition(KitchenTicketState currentState, KitchenTicketState targetState) {
        if (!VALID_TRANSITIONS.getOrDefault(currentState, List.of()).contains(targetState)) {
            throw new ConflictException("Invalid kitchen transition from " + currentState + " to " + targetState + ".");
        }
    }

    public String toPersistentStatus(KitchenTicketState state) {
        return switch (state) {
            case QUEUED -> "queued";
            case STARTED, COOKING -> "cooking";
            case READY_TO_SERVE -> "ready";
            case SERVED -> "served";
            case RETURNED -> "returned";
            case CANCELLED -> "blocked";
        };
    }
}
