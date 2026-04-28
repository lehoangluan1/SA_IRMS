package SA.irms.runtime.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"SA.irms.reservation", "SA.irms.common"})
@EnableScheduling
public class ReservationServiceApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ReservationServiceApplication.class);
        application.setAdditionalProfiles("reservation-service");
        application.run(args);
    }
}
