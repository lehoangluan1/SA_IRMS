package SA.irms.identity.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import SA.irms.identity.persistence.IdentityRepository;

@Component
class AuditCsvExporter {
    String export(List<IdentityRepository.AuditRow> auditRows) {
        StringBuilder builder = new StringBuilder();
        builder.append("recorded_at,action,actor,roles,entity_type,entity_id,correlation_id,follow_up,reason\n");
        for (IdentityRepository.AuditRow row : auditRows) {
            builder.append(csv(row.recordedAt().toString())).append(',')
                    .append(csv(row.action())).append(',')
                    .append(csv(row.actorName())).append(',')
                    .append(csv(String.join("|", row.roles()))).append(',')
                    .append(csv(row.entityType())).append(',')
                    .append(csv(row.entityId())).append(',')
                    .append(csv(row.correlationId())).append(',')
                    .append(row.followUp()).append(',')
                    .append(csv(row.reason()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
