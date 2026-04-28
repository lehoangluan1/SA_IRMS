package SA.irms.common.remote;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import SA.irms.common.config.AppProperties;
import SA.irms.common.security.InternalServiceAuthenticationFilter;

@Configuration
public class RestClientConfiguration {
    @Bean
    RestTemplate serviceRestTemplate(RestTemplateBuilder builder, AppProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(8))
                .additionalInterceptors((request, body, execution) -> {
                    String token = properties.security().internalServiceToken();
                    if (token != null && !token.isBlank()) {
                        request.getHeaders().set(InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER, token);
                    }
                    return execution.execute(request, body);
                }).build();
    }
}
