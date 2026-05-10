package SA.irms.support;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import SA.irms.common.error.GlobalExceptionHandler;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.web.CorrelationIdFilter;

public abstract class ApiContractTestSupport {
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    protected MockMvc mockMvcFor(Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .setValidator(validator)
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected AuthenticatedUser user(String... permissions) {
        return new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "manager@irms.local",
                "Alex Manager",
                Set.of("manager"),
                permissions.length == 0 ? Set.of("all") : Set.of(permissions),
                UUID.fromString("22222222-2222-2222-2222-222222222222")
        );
    }
}
