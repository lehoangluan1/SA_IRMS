package SA.irms.runtime.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.identity", "SA.irms.common"})
@EnableScheduling
public class IdentityAuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(IdentityAuditServiceApplication.class);
        application.setAdditionalProfiles("identity-audit-service");
        application.run(args);
    }
}
