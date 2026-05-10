package SA.irms.gateway;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
class GatewayHttpClientConfiguration {
    @Bean
    RestTemplate gatewayRestTemplate(RestTemplateBuilder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        return builder.requestFactory(() -> requestFactory).build();
    }
}
