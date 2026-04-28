package SA.irms.kitchen.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.port.out.KitchenTicketPriorityCommandPort;

@Repository
public class JdbcKitchenTicketPriorityCommandAdapter implements KitchenTicketPriorityCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcKitchenTicketPriorityCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<AutoPriorityCandidate> loadAutoPriorityCandidates() {
        return jdbcClient.sql("""
                        select ticket_id, priority_label, target_service_at
                        from kitchen_tickets
                        where status in ('queued', 'cooking')
                          and target_service_at is not null
                        """)
                .query((rs, rowNum) -> new AutoPriorityCandidate(
                        rs.getObject("ticket_id", UUID.class),
                        rs.getString("priority_label"),
                        rs.getTimestamp("target_service_at").toInstant()
                ))
                .list();
    }

    @Override
    public void updatePriority(UUID ticketId, String priorityLabel, int priorityScore, boolean keepExistingExpediteTimestamp) {
        String expeditedAtClause = keepExistingExpediteTimestamp
                ? "expedited_at = case when :priorityLabel = 'expedite' and expedited_at is null then now() else expedited_at end,"
                : "expedited_at = case when :priorityLabel = 'expedite' then now() else expedited_at end,";
        jdbcClient.sql("""
                        update kitchen_tickets
                        set priority_label = :priorityLabel,
                            priority = :priority,
                            %s
                            updated_at = now()
                        where ticket_id = :ticketId
                        """.formatted(expeditedAtClause))
                .param("priorityLabel", priorityLabel)
                .param("priority", priorityScore)
                .param("ticketId", ticketId)
                .update();
    }
}
