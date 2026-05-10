package SA.irms.common.outbox;

public interface OutboxRelay {
    int relayPendingEvents();
}
