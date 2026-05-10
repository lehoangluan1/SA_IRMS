package SA.irms.reservation.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.identity.SharedIdentityDirectoryPort;

@Component
public class ReservationSessionServerResolver {
    private final SharedIdentityDirectoryPort identityDirectoryPort;

    ReservationSessionServerResolver(SharedIdentityDirectoryPort identityDirectoryPort) {
        this.identityDirectoryPort = identityDirectoryPort;
    }

    UUID resolveSessionServer(AuthenticatedUser actor) {
        if (actor.hasRole("server")) {
            return actor.userId();
        }
        return identityDirectoryPort.findFirstActiveUserIdByRolePriority(List.of("server"))
                .orElseThrow();
    }
}
