package SA.irms.identity.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.StatusResponse;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.security.CurrentUser;
import SA.irms.common.web.RequestContext;
import SA.irms.identity.application.AuthService;
import SA.irms.identity.application.view.IdentityViews;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public ApiEnvelope<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        IdentityViews.LoginResult result = authService.login(
                request.email(),
                request.password(),
                request.role(),
                request.deviceId() == null || request.deviceId().isBlank() ? userAgent : request.deviceId(),
                clientIp(httpServletRequest)
        );
        return ApiEnvelope.of(
                new LoginResponse(result.token(), result.expiresAt(), result.user()),
                RequestContext.getCorrelationId(httpServletRequest)
        );
    }

    @PostMapping("/logout")
    public ApiEnvelope<StatusResponse> logout(HttpServletRequest request) {
        AuthenticatedUser user = currentUser.require();
        authService.logout(user);
        return ApiEnvelope.of(new StatusResponse("signed_out"), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/me")
    public ApiEnvelope<IdentityViews.UserView> me(HttpServletRequest request) {
        return ApiEnvelope.of(authService.currentUser(currentUser.require()), RequestContext.getCorrelationId(request));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String role,
            String deviceId
    ) {
    }

    public record LoginResponse(
            String token,
            java.time.Instant expiresAt,
            IdentityViews.UserView user
    ) {
    }
}
