package SA.irms.common.messaging;

import java.util.UUID;

interface ProcessedEventRepository {
    void delete(UUID eventId, String consumerName);
}
