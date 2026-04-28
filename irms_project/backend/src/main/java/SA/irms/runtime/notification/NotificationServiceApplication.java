package SA.irms.runtime.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.notification", "SA.irms.common"})
@EnableScheduling
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NotificationServiceApplication.class);
        application.setAdditionalProfiles("notification-service");
        application.run(args);
    }
}
