package SA.irms.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import SA.irms.common.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final AppProperties properties;

    public InternalServiceAuthenticationFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expectedToken = properties.security().internalServiceToken();
        String actualToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (expectedToken != null && !expectedToken.isBlank() && expectedToken.equals(actualToken)) {
            UUID serviceUserId = UUID.nameUUIDFromBytes("irms-internal-service".getBytes(StandardCharsets.UTF_8));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(
                            serviceUserId,
                            "internal-service",
                            "Internal Service",
                            Set.of("service"),
                            Set.of("all"),
                            null
                    ),
                    actualToken,
                    Set.of(new SimpleGrantedAuthority("all"), new SimpleGrantedAuthority("INTERNAL_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
