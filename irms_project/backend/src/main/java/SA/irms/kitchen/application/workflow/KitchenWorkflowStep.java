package SA.irms.kitchen.application.workflow;

public interface KitchenWorkflowStep<T> {
    String name();

    void execute(T context);
}
