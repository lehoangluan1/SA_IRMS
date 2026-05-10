package SA.irms.common.messaging;

public record RabbitMqPublishResult(Status status, String reason) {
    public enum Status {
        CONFIRMED_AND_ROUTED,
        CONFIRMED_BUT_RETURNED,
        NACKED,
        TIMEOUT,
        FAILED
    }

    public boolean confirmedAndRouted() {
        return status == Status.CONFIRMED_AND_ROUTED;
    }

    public boolean retryable() {
        return status != Status.CONFIRMED_AND_ROUTED;
    }

    public static RabbitMqPublishResult confirmedAndRoutedResult() {
        return new RabbitMqPublishResult(Status.CONFIRMED_AND_ROUTED, null);
    }

    public static RabbitMqPublishResult confirmedButReturned(String reason) {
        return new RabbitMqPublishResult(Status.CONFIRMED_BUT_RETURNED,
                reason == null || reason.isBlank() ? "RabbitMQ confirmed the publish but returned the message as unroutable." : reason);
    }

    public static RabbitMqPublishResult nacked(String reason) {
        return new RabbitMqPublishResult(Status.NACKED,
                reason == null || reason.isBlank() ? "RabbitMQ negatively acknowledged the publish." : reason);
    }

    public static RabbitMqPublishResult timeout(String reason) {
        return new RabbitMqPublishResult(Status.TIMEOUT,
                reason == null || reason.isBlank() ? "RabbitMQ publisher confirm timed out." : reason);
    }

    public static RabbitMqPublishResult failed(String reason) {
        return new RabbitMqPublishResult(Status.FAILED,
                reason == null || reason.isBlank() ? "RabbitMQ publish failed." : reason);
    }
}
