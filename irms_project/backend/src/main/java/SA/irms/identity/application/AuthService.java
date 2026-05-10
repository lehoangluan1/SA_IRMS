package SA.irms.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.config.AppProperties;
import SA.irms.common.error.ForbiddenException;
import SA.irms.common.error.UnauthorizedException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.security.SignedAccessTokenClaims;
import SA.irms.common.security.SignedAccessTokenService;
import SA.irms.common.security.TokenHashingService;
import SA.irms.identity.application.view.IdentityViews;
import SA.irms.identity.application.port.out.IdentitySessionRepositoryPort;
import SA.irms.identity.application.port.out.IdentityUserRepository;

@Service
public class AuthService {
    private final IdentityUserRepository identityUserRepository;
    private final IdentitySessionRepositoryPort identitySessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashingService tokenHashingService;
    private final SignedAccessTokenService signedAccessTokenService;
    private final AppProperties properties;
    private final Clock clock;

    public AuthService(
            IdentityUserRepository identityUserRepository,
            IdentitySessionRepositoryPort identitySessionRepository,
            PasswordEncoder passwordEncoder,
            TokenHashingService tokenHashingService,
            SignedAccessTokenService signedAccessTokenService,
            AppProperties properties,
            Clock clock
    ) {
        this.identityUserRepository = identityUserRepository;
        this.identitySessionRepository = identitySessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashingService = tokenHashingService;
        this.signedAccessTokenService = signedAccessTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IdentityViews.LoginResult login(String email, String password, String requestedRole, String deviceId, String ipAddress) {
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedRole = requestedRole == null ? null : requestedRole.trim();

        IdentityUserRepository.UserAccount account = identityUserRepository.findUserAccountByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!"active".equals(account.status())) {
            throw new ForbiddenException("This user account is not active.");
        }
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        if (normalizedRole != null && !normalizedRole.isBlank() && !account.roles().contains(normalizedRole)) {
            throw new ForbiddenException("The selected role is not assigned to this account.");
        }

        Instant now = Instant.now(clock);
        Instant sessionExpiresAt = now.plusSeconds((long) properties.security().sessionAbsoluteTimeoutHours() * 3600L);
        Instant accessTokenExpiresAt = now.plusSeconds((long) properties.security().accessTokenLifetimeMinutes() * 60L);
        if (accessTokenExpiresAt.isAfter(sessionExpiresAt)) {
            accessTokenExpiresAt = sessionExpiresAt;
        }
        UUID sessionId = UUID.randomUUID();
        String token = signedAccessTokenService.issue(new SignedAccessTokenClaims(
                sessionId,
                account.userId(),
                account.username(),
                account.displayName(),
                account.roles(),
                account.permissions(),
                now,
                accessTokenExpiresAt
        ));
        identitySessionRepository.createSession(
                sessionId,
                account.userId(),
                tokenHashingService.hash(token),
                now,
                sessionExpiresAt,
                limitText(deviceId, 150),
                limitText(ipAddress, 100)
        );
        identityUserRepository.updateLastLogin(account.userId(), now);

        return new IdentityViews.LoginResult(
                token,
                accessTokenExpiresAt,
                new IdentityViews.UserView(
                        account.userId(),
                        account.email(),
                        account.displayName(),
                        account.roles().stream().sorted().toList(),
                        account.permissions().stream().sorted().toList(),
                        sessionId
                )
        );
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @Transactional
    public void logout(AuthenticatedUser user) {
        identitySessionRepository.terminateSession(user.sessionId(), Instant.now(clock));
    }

    public IdentityViews.UserView currentUser(AuthenticatedUser user) {
        IdentityUserRepository.UserAccount account = identityUserRepository.findUserAccountById(user.userId())
                .orElseThrow(() -> new UnauthorizedException("Session user was not found."));
        return new IdentityViews.UserView(
                account.userId(),
                account.email(),
                account.displayName(),
                account.roles().stream().sorted().toList(),
                account.permissions().stream().sorted().toList(),
                user.sessionId()
        );
    }
}
