package SA.irms.runtime.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"SA.irms.common"})
public class MigrationApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MigrationApplication.class);
        application.setAdditionalProfiles("migration");
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    @Bean
    org.springframework.boot.ApplicationRunner migrationExitRunner(ApplicationContext applicationContext) {
        return args -> {
            SpringApplication.exit(applicationContext, () -> 0);
            System.exit(0);
        };
    }
}
