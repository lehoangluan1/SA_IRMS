package SA.irms.common.security;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ForbiddenException;

@Component
public class PermissionGuard {
    private final CurrentUser currentUser;

    public PermissionGuard(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public AuthenticatedUser require(String permission) {
        AuthenticatedUser user = currentUser.require();
        if (!user.hasPermission(permission)) {
            throw new ForbiddenException("You do not have permission to perform this action.");
        }
        return user;
    }
}
