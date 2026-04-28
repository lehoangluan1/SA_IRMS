package SA.irms.identity.application.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import SA.irms.identity.application.port.out.IdentityStaffRepositoryPort;

public final class IdentityViews {
    private IdentityViews() {
    }

    public record UserView(UUID userId, String email, String displayName, List<String> roles, List<String> permissions, UUID sessionId) {
    }

    public record LoginResult(String token, Instant expiresAt, UserView user) {
    }

    public record StaffView(
            List<IdentityStaffRepositoryPort.StaffRow> staff,
            List<IdentityStaffRepositoryPort.RoleRow> roles,
            List<IdentityStaffRepositoryPort.ShiftRow> shifts
    ) {
    }

    public record StaffRowView(UUID userId, String displayName, String email, String status, LocalDate hireDate, Set<String> roles, Set<String> permissions) {
    }

    public record ShiftView(UUID shiftAssignmentId, UUID userId, String displayName, String roleName, LocalDate shiftDate, LocalTime startAt, LocalTime endAt, String position, String zone, String status) {
    }

    public record FailedOutboxEventView(
            UUID eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String producerService,
            String exchangeName,
            String routingKey,
            int retryCount,
            String lastError,
            Instant createdAt
    ) {
    }

    public record DeadLetterEventView(
            UUID deadLetterId,
            UUID eventId,
            String consumerName,
            String eventType,
            String failureReason,
            String status,
            Instant createdAt
    ) {
    }
}
