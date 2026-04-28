package SA.irms.common.identity;

import java.time.Instant;

public record TouchSessionRequest(Instant lastActivityAt) {
}
