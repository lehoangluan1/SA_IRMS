package SA.irms.kitchen.application.support;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import SA.irms.common.config.AppProperties;

@Component
public class KitchenTimingPolicy {
    private final AppProperties appProperties;

    public KitchenTimingPolicy(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public int randomQueueDelaySeconds() {
        return ThreadLocalRandom.current().nextInt(15, 46);
    }

    public int randomCookingSeconds() {
        int baseSeconds = Math.max(90, appProperties.kitchenAutomation().cookingToReadySeconds() - randomQueueDelaySeconds());
        int jitter = Math.min(30, Math.max(10, baseSeconds / 5));
        return ThreadLocalRandom.current().nextInt(Math.max(60, baseSeconds - jitter), baseSeconds + jitter + 1);
    }

    public int randomReadyToServedSeconds() {
        int baseSeconds = Math.max(45, appProperties.kitchenAutomation().readyToServedSeconds());
        int jitter = Math.min(20, Math.max(10, baseSeconds / 6));
        return ThreadLocalRandom.current().nextInt(Math.max(30, baseSeconds - jitter), baseSeconds + jitter + 1);
    }
}
