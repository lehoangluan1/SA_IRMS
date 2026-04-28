package SA.irms.common.security;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import SA.irms.common.api.ApiErrorResponse;
import SA.irms.common.config.AppProperties;
import SA.irms.common.web.RequestContext;
import SA.irms.common.identity.SharedIdentitySessionPort;

@Configuration
@Profile("!migration")
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            AppProperties properties,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/health").permitAll()
                        .requestMatchers("/internal/**").hasAuthority("INTERNAL_SERVICE")
                        .anyRequest().authenticated())
                .addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                "unauthorized",
                                "Authentication is required.",
                                RequestContext.getCorrelationId(request)
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                "forbidden",
                                "You do not have access to this operation.",
                                RequestContext.getCorrelationId(request)
                        )));
        return http.build();
    }

    @Bean
    InternalServiceAuthenticationFilter internalServiceAuthenticationFilter(AppProperties properties) {
        return new InternalServiceAuthenticationFilter(properties);
    }

    @Bean
    SessionAuthenticationFilter sessionAuthenticationFilter(
            SharedIdentitySessionPort identitySessionPort,
            TokenHashingService tokenHashingService,
            SignedAccessTokenService signedAccessTokenService,
            SessionTouchRecorder sessionTouchRecorder,
            SessionRevocationGuard sessionRevocationGuard,
            AppProperties properties
    ) {
        return new SessionAuthenticationFilter(identitySessionPort, tokenHashingService, signedAccessTokenService, sessionTouchRecorder, sessionRevocationGuard, properties);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.copyOf(properties.cors().allowedOrigins()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setExposedHeaders(List.of(RequestContext.CORRELATION_ID_HEADER));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(
            jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String code,
            String message,
            String correlationId
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(code, message, correlationId, java.time.Instant.now(), List.of())
        );
    }
}
