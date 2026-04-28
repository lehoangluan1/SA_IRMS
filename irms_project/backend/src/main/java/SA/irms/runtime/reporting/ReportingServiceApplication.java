package SA.irms.runtime.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.reporting", "SA.irms.common"})
@EnableScheduling
public class ReportingServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ReportingServiceApplication.class);
        application.setAdditionalProfiles("reporting-service");
        application.run(args);
    }
}
