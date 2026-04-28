package SA.irms.common.infrastructure.persistence;

import SA.irms.common.workflow.WorkflowStepRecorder;
import SA.irms.common.workflow.WorkflowStepStatus;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcWorkflowStepRecorder implements WorkflowStepRecorder {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcWorkflowStepRecorder(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(String workflowType, String aggregateType, String aggregateId, String stepName,
                       WorkflowStepStatus status, UUID eventId, String error, Map<String, Object> payload) {
        jdbcClient.sql("""
                        insert into workflow_steps (
                            workflow_step_id,
                            workflow_id,
                            workflow_type,
                            aggregate_type,
                            aggregate_id,
                            step_name,
                            status,
                            event_id,
                            error,
                            payload,
                            started_at,
                            completed_at
                        )
                        select :workflowStepId,
                               wi.workflow_id,
                               :workflowType,
                               :aggregateType,
                               :aggregateId,
                               :stepName,
                               :status,
                               :eventId,
                               :error,
                               cast(:payload as jsonb),
                               now(),
                               case when :status in ('COMPLETED', 'FAILED', 'SKIPPED', 'COMPENSATED') then now() else null end
                        from workflow_instances wi
                        where wi.workflow_type = :workflowType
                          and wi.aggregate_type = :aggregateType
                          and wi.aggregate_id = :aggregateId
                        """)
                .param("workflowStepId", UUID.randomUUID())
                .param("workflowType", workflowType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .param("stepName", stepName)
                .param("status", status.name())
                .param("eventId", eventId)
                .param("error", truncate(error))
                .param("payload", toJson(payload))
                .update();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Workflow step payload could not be serialized.", exception);
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
