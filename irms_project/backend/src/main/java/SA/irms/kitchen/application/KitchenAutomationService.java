package SA.irms.kitchen.application;

import SA.irms.kitchen.application.support.KitchenAutomationStep;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenAutomationService {
    private final List<KitchenAutomationStep> steps;

    KitchenAutomationService(List<KitchenAutomationStep> steps) {
        this.steps = steps;
    }

    @Transactional
    public void advanceKitchenAutomation() {
        steps.forEach(KitchenAutomationStep::execute);
    }
}
