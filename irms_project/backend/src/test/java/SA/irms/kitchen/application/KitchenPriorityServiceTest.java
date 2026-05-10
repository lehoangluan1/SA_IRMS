package SA.irms.kitchen.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.kitchen.application.port.out.KitchenTicketPriorityCommandPort;
import SA.irms.kitchen.application.port.out.KitchenTicketQueryRepository;
import SA.irms.kitchen.domain.KitchenPriorityPolicy;

@ExtendWith(MockitoExtension.class)
class KitchenPriorityServiceTest {

    @Mock
    private KitchenTicketPriorityCommandPort priorityCommandPort;
    @Mock
    private SharedIdentityPolicyPort identityPolicyPort;
    @Mock
    private SharedIdentityDirectoryPort identityDirectoryPort;
    @Mock
    private AuditRecorder auditRecorder;
    @Mock
    private NotificationCommandPublisher notificationPublisher;
    @Mock
    private KitchenTicketQueryRepository ticketQueryRepository;

    private KitchenPriorityService service;

    private final Instant now = Instant.parse("2026-05-10T10:00:00Z");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        service = new KitchenPriorityService(
                priorityCommandPort,
                identityPolicyPort,
                identityDirectoryPort,
                auditRecorder,
                notificationPublisher,
                ticketQueryRepository,
                new KitchenPriorityPolicy(),
                fixedClock
        );

        when(identityDirectoryPort.findFirstActiveUserIdByRolePriority(any()))
                .thenReturn(Optional.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));
        when(identityPolicyPort.getPolicySnapshot()).thenReturn(policySnapshot(15, 20));
    }

    @Test
    void normalTicketInsideRushWindowEscalatesToRushNotExpedite() {
        UUID ticketId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(priorityCommandPort.loadAutoPriorityCandidates()).thenReturn(List.of(
                new KitchenTicketPriorityCommandPort.AutoPriorityCandidate(ticketId, "normal", now.plusSeconds(10 * 60L))
        ));

        service.applyAutomaticPriorityEscalation();

        verify(priorityCommandPort).updatePriority(ticketId, "rush", 8, true);
        verify(notificationPublisher, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void rushTicketOverdueBeyondLateThresholdEscalatesToExpedite() {
        UUID ticketId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(priorityCommandPort.loadAutoPriorityCandidates()).thenReturn(List.of(
                new KitchenTicketPriorityCommandPort.AutoPriorityCandidate(ticketId, "rush", now.minusSeconds(25 * 60L))
        ));

        service.applyAutomaticPriorityEscalation();

        verify(priorityCommandPort).updatePriority(ticketId, "expedite", 10, true);
                verify(notificationPublisher, times(2)).enqueue(any(), eq("KitchenTicket"), eq(ticketId.toString()), eq("system:auto-priority"));
    }

    private PolicySnapshot policySnapshot(int rushThresholdMin, int lateThresholdMin) {
        return new PolicySnapshot(
                new PolicySnapshot.AuthorizationPolicy(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        "default",
                        true,
                        BigDecimal.TEN,
                        24,
                        15,
                        12
                ),
                new PolicySnapshot.SeatingPolicy(
                        UUID.fromString("20000000-0000-0000-0000-000000000001"),
                        15,
                        BigDecimal.ONE,
                        15
                ),
                new PolicySnapshot.PricingPolicy(
                        UUID.fromString("30000000-0000-0000-0000-000000000001"),
                        "default",
                        BigDecimal.ZERO,
                        true
                ),
                new PolicySnapshot.TaxPolicy(
                        UUID.fromString("40000000-0000-0000-0000-000000000001"),
                        "default",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true
                ),
                new PolicySnapshot.PreparationPolicy(
                        UUID.fromString("50000000-0000-0000-0000-000000000001"),
                        true,
                        true,
                        rushThresholdMin
                ),
                new PolicySnapshot.ExpediteRule(
                        UUID.fromString("60000000-0000-0000-0000-000000000001"),
                        10,
                        lateThresholdMin,
                        true
                )
        );
    }
}
