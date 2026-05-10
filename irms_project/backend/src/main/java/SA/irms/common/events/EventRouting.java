package SA.irms.common.events;

public record EventRouting(String exchangeName, String routingKey) {
    public EventRouting {
        if (exchangeName == null || exchangeName.isBlank()) {
            throw new IllegalArgumentException("exchangeName is required.");
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalArgumentException("routingKey is required.");
        }
    }
}
