package SA.irms.identity.audit;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.identity.persistence.IdentityRepository;

@Component
class AuditMaskingPolicy {
    IdentityRepository.AuditRow maskForViewer(IdentityRepository.AuditRow row) {
        return new IdentityRepository.AuditRow(
                row.auditLogId(),
                row.recordedAt(),
                row.actorUserId(),
                row.actorName(),
                row.roles(),
                row.action(),
                row.entityType(),
                row.entityId(),
                row.correlationId(),
                row.reason(),
                row.followUp(),
                row.ipAddress(),
                maskMap(row.beforePayload()),
                maskMap(row.afterPayload())
        );
    }

    private Map<String, Object> maskMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> masked = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            masked.put(entry.getKey(), maskValue(entry.getKey(), entry.getValue()));
        }
        return masked;
    }

    @SuppressWarnings("unchecked")
    private Object maskValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        String normalizedKey = key == null ? "" : key.toLowerCase();
        if (normalizedKey.contains("email")
                || normalizedKey.contains("phone")
                || normalizedKey.contains("address")
                || normalizedKey.contains("password")
                || normalizedKey.contains("token")) {
            return "***";
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nested = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), maskValue(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return nested;
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream()
                    .map(item -> item instanceof Map<?, ?> || item instanceof List<?>
                            ? maskValue(key, item)
                            : item)
                    .toList();
        }
        return value;
    }
}
