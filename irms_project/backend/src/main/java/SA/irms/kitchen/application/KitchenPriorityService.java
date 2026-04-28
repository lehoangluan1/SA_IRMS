package SA.irms.kitchen.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.kitchen.application.port.out.KitchenTicketPriorityCommandPort;
import SA.irms.kitchen.application.port.out.KitchenTicketQueryRepository;
import SA.irms.kitchen.domain.KitchenPriorityPolicy;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.SharedIdentityDirectoryPort;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.notification.NotificationCommand;
import SA.irms.common.notification.NotificationCommandPublisher;

@Service
public class KitchenPriorityService implements SA.irms.kitchen.application.port.out.KitchenAutomationActorPort {
    private final KitchenTicketPriorityCommandPort priorityCommandPort;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final SharedIdentityDirectoryPort identityDirectoryPort;
    private final AuditRecorder auditService;
    private final NotificationCommandPublisher notificationOutboxPublisher;
    private final KitchenTicketQueryRepository kitchenTicketReadService;
    private final KitchenPriorityPolicy kitchenPriorityPolicy;
    private final Clock clock;

    public KitchenPriorityService(
            KitchenTicketPriorityCommandPort priorityCommandPort,
            SharedIdentityPolicyPort identityPolicyPort,
            SharedIdentityDirectoryPort identityDirectoryPort,
            AuditRecorder auditService,
            NotificationCommandPublisher notificationOutboxPublisher,
            KitchenTicketQueryRepository kitchenTicketReadService,
            KitchenPriorityPolicy kitchenPriorityPolicy,
            Clock clock
    ) {
        this.priorityCommandPort = priorityCommandPort;
        this.identityPolicyPort = identityPolicyPort;
        this.identityDirectoryPort = identityDirectoryPort;
        this.auditService = auditService;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.kitchenTicketReadService = kitchenTicketReadService;
        this.kitchenPriorityPolicy = kitchenPriorityPolicy;
        this.clock = clock;
    }

    @Transactional
    public SA.irms.kitchen.application.view.KitchenViews.TicketView togglePriority(UUID ticketId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) {
        PolicySnapshot.ExpediteRule expediteRule = identityPolicyPort.getPolicySnapshot().expediteRule();
        SA.irms.kitchen.application.view.KitchenViews.TicketView ticket = kitchenTicketReadService.findTicket(ticketId);
        String nextPriority = kitchenPriorityPolicy.nextManualPriority(ticket.priority());
        if (!"normal".equals(nextPriority) && (reason == null || reason.isBlank())) {
            throw new ConflictException("A priority reason is required.");
        }
        if ("expedite".equals(nextPriority)
                && expediteRule.requiresManagerApproval()
                && !actor.hasRole("manager")
                && !actor.hasRole("admin")) {
            throw new ConflictException("Manager approval is required before expediting a ticket.");
        }
        priorityCommandPort.updatePriority(ticketId, nextPriority, kitchenPriorityPolicy.score(nextPriority), false);
        auditService.record(actor.userId(), "kitchen.priority.changed", "KitchenTicket", ticketId.toString(), correlationId,
                reason, false, request.remoteIp(), Map.of("priority", ticket.priority()),
                "normal".equals(nextPriority) ? Map.of("priority", nextPriority) : Map.of("priority", nextPriority, "reason", reason));
        return kitchenTicketReadService.findTicket(ticketId);
    }

    @Transactional
    public void applyAutomaticPriorityEscalation() {
        PolicySnapshot policies = identityPolicyPort.getPolicySnapshot();
        Instant now = Instant.now(clock);
        Instant rushCutoff = now.plusSeconds((long) policies.preparationPolicy().rushThresholdMin() * 60L);
        Instant expediteCutoff = now.plusSeconds((long) policies.expediteRule().lateThresholdMin() * 60L);
        UUID auditActorUserId = resolveAutomaticPriorityActorUserId();
        for (KitchenTicketPriorityCommandPort.AutoPriorityCandidate candidate : priorityCommandPort.loadAutoPriorityCandidates()) {
            String nextPriority = kitchenPriorityPolicy.automaticEscalation(candidate.priorityLabel(), candidate.targetServiceAt(), rushCutoff, expediteCutoff);
            if (nextPriority == null) {
                continue;
            }
            priorityCommandPort.updatePriority(candidate.ticketId(), nextPriority, kitchenPriorityPolicy.score(nextPriority), true);
            auditService.record(auditActorUserId, "kitchen.priority.auto_escalated", "KitchenTicket", candidate.ticketId().toString(),
                    "system:auto-priority", "Automatic near-SLA escalation.", false, null,
                    Map.of("priority", candidate.priorityLabel()), Map.of("priority", nextPriority));
            if ("expedite".equals(nextPriority)) {
                queueOverdueNotification(candidate.ticketId());
            }
        }
    }

    public UUID resolveAutomaticPriorityActorUserId() {
        return identityDirectoryPort.findFirstActiveUserIdByRolePriority(List.of("admin", "manager", "chef"))
                .orElseThrow(() -> new ConflictException("No kitchen automation account is available."));
    }

    public AuthenticatedUser automaticKitchenActor() {
        UUID actorUserId = resolveAutomaticPriorityActorUserId();
        return new AuthenticatedUser(actorUserId, "system-kitchen", "Kitchen Automation", java.util.Set.of("chef"), java.util.Set.of("kitchen.manage"), null);
    }

    private void queueOverdueNotification(UUID ticketId) {
        for (String role : List.of("manager", "chef")) {
            notificationOutboxPublisher.enqueue(new NotificationCommand(null, null, null, null, "in_app", "food_overdue",
                    "KITCHEN_OVERDUE", Map.of("ticketId", ticketId.toString()), "Kitchen ticket overdue",
                    "A kitchen ticket needs immediate attention.", role, null, null, "high"),
                    "KitchenTicket", ticketId.toString(), "system:auto-priority");
        }
    }
}
