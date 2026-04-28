package SA.irms.runtime.kitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.kitchen", "SA.irms.common"})
@EnableScheduling
public class KitchenServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(KitchenServiceApplication.class);
        application.setAdditionalProfiles("kitchen-service");
        application.run(args);
    }
}
