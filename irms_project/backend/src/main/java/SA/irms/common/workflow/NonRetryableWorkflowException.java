package SA.irms.common.workflow;

public class NonRetryableWorkflowException extends RuntimeException {
    public NonRetryableWorkflowException(String message) {
        super(message);
    }

    public NonRetryableWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
