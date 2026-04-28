package SA.irms.common.workflow;

public class RetryableWorkflowException extends RuntimeException {
    public RetryableWorkflowException(String message) {
        super(message);
    }

    public RetryableWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
