package SA.irms.common.messaging;

public final class RabbitMqMessageHeaders {
    private RabbitMqMessageHeaders() {
    }

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_VERSION = "eventVersion";
    public static final String AGGREGATE_TYPE = "aggregateType";
    public static final String AGGREGATE_ID = "aggregateId";
    public static final String PRODUCER_SERVICE = "producerService";
    public static final String CORRELATION_ID = "correlationId";
    public static final String CAUSATION_ID = "causationId";
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String OCCURRED_AT = "occurredAt";
}
