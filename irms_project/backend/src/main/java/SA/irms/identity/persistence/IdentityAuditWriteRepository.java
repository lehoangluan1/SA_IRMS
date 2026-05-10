package SA.irms.identity.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.json.JsonSupport;

@Repository
class IdentityAuditWriteRepository {
    private final JdbcClient jdbcClient;
    private final JsonSupport jsonSupport;

    IdentityAuditWriteRepository(JdbcClient jdbcClient, JsonSupport jsonSupport) {
        this.jdbcClient = jdbcClient;
        this.jsonSupport = jsonSupport;
    }

    void updateAuditFollowUp(UUID auditLogId, boolean followUp) {
        jdbcClient.sql("""
                        update audit_logs
                        set follow_up = :followUp
                        where audit_log_id = :auditLogId
                        """)
                .param("followUp", followUp)
                .param("auditLogId", auditLogId)
                .update();
    }

    boolean insertAudit(IdentityRepository.AuditCommand command) {
        int inserted = jdbcClient.sql("""
                        insert into audit_logs (
                            audit_log_id,
                            source_event_id,
                            actor_user_id,
                            action,
                            entity_type,
                            entity_id,
                            recorded_at,
                            correlation_id,
                            reason,
                            follow_up,
                            ip_address,
                            before_payload,
                            after_payload
                        ) values (
                            :auditLogId,
                            :sourceEventId,
                            :actorUserId,
                            :action,
                            :entityType,
                            :entityId,
                            :recordedAt,
                            :correlationId,
                            :reason,
                            :followUp,
                            :ipAddress,
                            cast(:beforePayload as jsonb),
                            cast(:afterPayload as jsonb)
                        )
                        on conflict (source_event_id) where source_event_id is not null do nothing
                        """)
                .param("auditLogId", command.auditLogId())
                .param("sourceEventId", command.sourceEventId())
                .param("actorUserId", command.actorUserId())
                .param("action", command.action())
                .param("entityType", command.entityType())
                .param("entityId", command.entityId())
                .param("recordedAt", toSqlTimestamp(command.recordedAt()))
                .param("correlationId", command.correlationId())
                .param("reason", command.reason())
                .param("followUp", command.followUp())
                .param("ipAddress", command.ipAddress())
                .param("beforePayload", jsonSupport.write(command.beforePayload()))
                .param("afterPayload", jsonSupport.write(command.afterPayload()))
                .update();
        if (inserted == 0) {
            return false;
        }
        for (IdentityRepository.AuditDetailCommand detail : command.details()) {
            jdbcClient.sql("""
                            insert into audit_details (
                                detail_id,
                                audit_log_id,
                                field_name,
                                before_value,
                                after_value
                            ) values (
                                :detailId,
                                :auditLogId,
                                :fieldName,
                                :beforeValue,
                                :afterValue
                            )
                            """)
                    .param("detailId", detail.detailId())
                    .param("auditLogId", command.auditLogId())
                    .param("fieldName", detail.fieldName())
                    .param("beforeValue", detail.beforeValue())
                    .param("afterValue", detail.afterValue())
                    .update();
        }
        return true;
    }

    private Timestamp toSqlTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
