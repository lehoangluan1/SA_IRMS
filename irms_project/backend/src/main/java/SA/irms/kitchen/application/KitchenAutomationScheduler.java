package SA.irms.kitchen.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenAutomationScheduler {
    private final KitchenAutomationService kitchenAutomationService;

    KitchenAutomationScheduler(KitchenAutomationService kitchenAutomationService) {
        this.kitchenAutomationService = kitchenAutomationService;
    }

    @Scheduled(fixedDelayString = "#{${irms.kitchen-automation.poll-seconds:30} * 1000}")
    @Transactional
    public void advanceKitchenAutomation() {
        kitchenAutomationService.advanceKitchenAutomation();
    }
}
