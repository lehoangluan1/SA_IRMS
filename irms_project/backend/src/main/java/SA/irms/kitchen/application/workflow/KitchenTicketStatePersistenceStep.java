package SA.irms.kitchen.application.workflow;

import org.springframework.stereotype.Component;

import SA.irms.kitchen.application.port.out.KitchenTicketWorkflowCommandPort;
import SA.irms.kitchen.domain.service.KitchenTicketTransitionPolicy;

@Component
public class KitchenTicketStatePersistenceStep implements KitchenWorkflowStep<KitchenTicketTransitionContext> {
    private final KitchenTicketWorkflowCommandPort commandPort;
    private final KitchenTicketTransitionPolicy transitionPolicy;

    public KitchenTicketStatePersistenceStep(KitchenTicketWorkflowCommandPort commandPort, KitchenTicketTransitionPolicy transitionPolicy) {
        this.commandPort = commandPort;
        this.transitionPolicy = transitionPolicy;
    }

    @Override
    public String name() {
        return "KITCHEN_TICKET_STATE_PERSISTED";
    }

    @Override
    public void execute(KitchenTicketTransitionContext context) {
        commandPort.updateTicketState(context.ticketId(), transitionPolicy.toPersistentStatus(context.targetState()), context.deadline());
    }
}
