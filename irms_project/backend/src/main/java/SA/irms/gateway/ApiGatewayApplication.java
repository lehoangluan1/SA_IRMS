package SA.irms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        scanBasePackages = {
                "SA.irms.gateway",
                "SA.irms.common.config",
                "SA.irms.common.error",
                "SA.irms.common.identity",
                "SA.irms.common.remote",
                "SA.irms.common.security",
                "SA.irms.common.web"
        },
        exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class}
)
@EnableConfigurationProperties(GatewayRoutesProperties.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ApiGatewayApplication.class);
        application.setAdditionalProfiles("api-gateway");
        application.run(args);
    }
}
