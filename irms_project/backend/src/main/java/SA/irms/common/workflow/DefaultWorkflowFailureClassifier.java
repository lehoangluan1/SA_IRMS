package SA.irms.common.workflow;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkflowFailureClassifier implements WorkflowFailureClassifier {
    @Override
    public FailureType classify(Throwable failure) {
        if (failure instanceof NonRetryableWorkflowException) {
            return FailureType.NON_RETRYABLE;
        }
        if (failure instanceof RetryableWorkflowException || failure instanceof TransientDataAccessException) {
            return FailureType.RETRYABLE;
        }
        return FailureType.NON_RETRYABLE;
    }
}
