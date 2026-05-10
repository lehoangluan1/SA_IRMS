package SA.irms.runtime.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.billing", "SA.irms.common", "SA.irms.adapters.pdf"})
@EnableScheduling
public class BillingServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BillingServiceApplication.class);
        application.setAdditionalProfiles("billing-service");
        application.run(args);
    }
}
