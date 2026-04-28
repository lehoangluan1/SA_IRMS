package SA.irms.ordering.application.workflow;

import java.util.Map;

public interface OrderFulfillmentStep<T> {
    String name();

    Map<String, Object> execute(T context);
}
