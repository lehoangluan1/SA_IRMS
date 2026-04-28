package SA.irms.kitchen.application;

import SA.irms.kitchen.application.support.KitchenAutomationStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class KitchenAutomaticPriorityEscalationStep implements KitchenAutomationStep {
    private final KitchenPriorityService priorityService;

    KitchenAutomaticPriorityEscalationStep(KitchenPriorityService priorityService) {
        this.priorityService = priorityService;
    }

    public void execute() {
        priorityService.applyAutomaticPriorityEscalation();
    }
}
