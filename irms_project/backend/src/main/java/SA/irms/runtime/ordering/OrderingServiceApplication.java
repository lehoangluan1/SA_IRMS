package SA.irms.runtime.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.ordering", "SA.irms.common"})
@EnableScheduling
public class OrderingServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(OrderingServiceApplication.class);
        application.setAdditionalProfiles("ordering-service");
        application.run(args);
    }
}
