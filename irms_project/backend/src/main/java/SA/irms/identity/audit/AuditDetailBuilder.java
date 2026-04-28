package SA.irms.identity.audit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.identity.persistence.IdentityRepository;

@Component
class AuditDetailBuilder {
    List<IdentityRepository.AuditDetailCommand> buildDetails(
            Map<String, Object> beforePayload,
            Map<String, Object> afterPayload
    ) {
        Map<String, Object> orderedKeys = new java.util.TreeMap<>();
        orderedKeys.putAll(beforePayload == null ? Map.of() : beforePayload);
        orderedKeys.putAll(afterPayload == null ? Map.of() : afterPayload);
        return orderedKeys.keySet()
                .stream()
                .map(key -> new IdentityRepository.AuditDetailCommand(
                        UUID.randomUUID(),
                        key,
                        beforePayload == null || !beforePayload.containsKey(key) ? null : String.valueOf(beforePayload.get(key)),
                        afterPayload == null || !afterPayload.containsKey(key) ? null : String.valueOf(afterPayload.get(key))
                ))
                .toList();
    }
}
