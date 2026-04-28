package SA.irms.runtime.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.inventory", "SA.irms.common"})
@EnableScheduling
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(InventoryServiceApplication.class);
        application.setAdditionalProfiles("inventory-service");
        application.run(args);
    }
}
