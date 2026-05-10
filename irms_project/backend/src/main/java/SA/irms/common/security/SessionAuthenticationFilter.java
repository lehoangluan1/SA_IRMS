package SA.irms.common.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import SA.irms.common.config.AppProperties;
import SA.irms.common.identity.SessionPrincipal;
import SA.irms.common.identity.SharedIdentitySessionPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SharedIdentitySessionPort identitySessionPort;
    private final TokenHashingService tokenHashingService;
    private final SignedAccessTokenService signedAccessTokenService;
    private final SessionTouchRecorder sessionTouchRecorder;
    private final SessionRevocationGuard sessionRevocationGuard;
    private final AppProperties properties;

    public SessionAuthenticationFilter(
            SharedIdentitySessionPort identitySessionPort,
            TokenHashingService tokenHashingService,
            SignedAccessTokenService signedAccessTokenService,
            SessionTouchRecorder sessionTouchRecorder,
            SessionRevocationGuard sessionRevocationGuard,
            AppProperties properties
    ) {
        this.identitySessionPort = identitySessionPort;
        this.tokenHashingService = tokenHashingService;
        this.signedAccessTokenService = signedAccessTokenService;
        this.sessionTouchRecorder = sessionTouchRecorder;
        this.sessionRevocationGuard = sessionRevocationGuard;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            authenticateSignedToken(token).or(() -> authenticateLegacyOpaqueToken(token))
                    .ifPresent(session -> {
                        sessionTouchRecorder.recordActivity(session.sessionId());
                        Set<SimpleGrantedAuthority> authorities = session.permissions()
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(java.util.stream.Collectors.toSet());
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(
                                        session.userId(),
                                        session.username(),
                                        session.displayName(),
                                        session.roles(),
                                        session.permissions(),
                                        session.sessionId()
                                ),
                                token,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private java.util.Optional<SessionPrincipal> authenticateSignedToken(String token) {
        return signedAccessTokenService.verify(token, Instant.now())
                .filter(claims -> sessionRevocationGuard.isSessionStillActive(token, claims.sessionId()))
                .map(claims -> new SessionPrincipal(
                        claims.sessionId(),
                        claims.userId(),
                        claims.username(),
                        claims.displayName(),
                        claims.roles(),
                        claims.permissions(),
                        claims.expiresAt(),
                        claims.issuedAt()
                ));
    }

    private java.util.Optional<SessionPrincipal> authenticateLegacyOpaqueToken(String token) {
        String tokenHash = tokenHashingService.hash(token);
        return identitySessionPort.findActiveSessionByTokenHash(tokenHash)
                .filter(session -> isSessionActive(session, Instant.now()));
    }

    private boolean isSessionActive(SessionPrincipal session, Instant now) {
        Instant idleCutoff = now.minusSeconds((long) properties.security().sessionIdleTimeoutMinutes() * 60L);
        return session.expiresAt().isAfter(now) && session.lastActivityAt().isAfter(idleCutoff);
    }
}
