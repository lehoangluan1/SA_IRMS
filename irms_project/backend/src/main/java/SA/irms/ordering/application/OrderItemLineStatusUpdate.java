package SA.irms.ordering.application;

import java.util.UUID;

public record OrderItemLineStatusUpdate(UUID orderItemId, String lineStatus) {
}
