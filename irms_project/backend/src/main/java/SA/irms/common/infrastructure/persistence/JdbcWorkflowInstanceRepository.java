package SA.irms.common.infrastructure.persistence;

import SA.irms.common.workflow.WorkflowInstance;
import SA.irms.common.workflow.WorkflowInstanceRepository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcWorkflowInstanceRepository implements WorkflowInstanceRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcWorkflowInstanceRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean start(String workflowType, String aggregateType, String aggregateId, String initialState,
                         String correlationId, UUID eventId, Map<String, Object> payload) {
        int inserted = jdbcClient.sql("""
                        insert into workflow_instances (
                            workflow_id,
                            workflow_type,
                            aggregate_type,
                            aggregate_id,
                            state,
                            version,
                            correlation_id,
                            last_event_id,
                            payload,
                            created_at,
                            updated_at
                        ) values (
                            :workflowId,
                            :workflowType,
                            :aggregateType,
                            :aggregateId,
                            :state,
                            0,
                            :correlationId,
                            :eventId,
                            cast(:payload as jsonb),
                            now(),
                            now()
                        )
                        on conflict (workflow_type, aggregate_type, aggregate_id) do nothing
                        """)
                .param("workflowId", UUID.randomUUID())
                .param("workflowType", workflowType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .param("state", initialState)
                .param("correlationId", correlationId)
                .param("eventId", eventId)
                .param("payload", toJson(payload))
                .update();
        return inserted == 1;
    }

    @Override
    public Optional<WorkflowInstance> find(String workflowType, String aggregateType, String aggregateId) {
        return jdbcClient.sql("""
                        select workflow_id, workflow_type, aggregate_type, aggregate_id, state, version,
                               correlation_id, failure_reason, last_event_id, payload, created_at, updated_at
                        from workflow_instances
                        where workflow_type = :workflowType
                          and aggregate_type = :aggregateType
                          and aggregate_id = :aggregateId
                        """)
                .param("workflowType", workflowType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .query((rs, rowNum) -> new WorkflowInstance(
                        rs.getObject("workflow_id", UUID.class),
                        rs.getString("workflow_type"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("state"),
                        rs.getInt("version"),
                        rs.getString("correlation_id"),
                        rs.getString("failure_reason"),
                        rs.getObject("last_event_id", UUID.class),
                        fromJson(rs.getString("payload")),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant()
                ))
                .optional();
    }

    @Override
    public void transition(String workflowType, String aggregateType, String aggregateId, String state,
                           UUID eventId, Map<String, Object> payload, String failureReason) {
        jdbcClient.sql("""
                        update workflow_instances
                        set state = :state,
                            version = version + 1,
                            last_event_id = :eventId,
                            payload = cast(:payload as jsonb),
                            failure_reason = :failureReason,
                            updated_at = now()
                        where workflow_type = :workflowType
                          and aggregate_type = :aggregateType
                          and aggregate_id = :aggregateId
                        """)
                .param("state", state)
                .param("eventId", eventId)
                .param("payload", toJson(payload))
                .param("failureReason", truncate(failureReason))
                .param("workflowType", workflowType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .update();
    }

    @Override
    public void markReplayRequested(String workflowType, String aggregateType, String aggregateId, String reason) {
        jdbcClient.sql("""
                        update workflow_instances
                        set state = 'REPLAY_REQUESTED',
                            failure_reason = :reason,
                            updated_at = now()
                        where workflow_type = :workflowType
                          and aggregate_type = :aggregateType
                          and aggregate_id = :aggregateId
                        """)
                .param("reason", truncate(reason))
                .param("workflowType", workflowType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .update();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Workflow payload could not be serialized.", exception);
        }
    }

    private Map<String, Object> fromJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Workflow payload could not be deserialized.", exception);
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
