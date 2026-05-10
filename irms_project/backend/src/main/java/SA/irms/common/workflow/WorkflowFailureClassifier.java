package SA.irms.common.workflow;

public interface WorkflowFailureClassifier {
    FailureType classify(Throwable failure);

    enum FailureType {
        RETRYABLE,
        NON_RETRYABLE
    }
}
