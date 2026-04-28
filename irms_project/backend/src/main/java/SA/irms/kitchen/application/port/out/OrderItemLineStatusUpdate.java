package SA.irms.kitchen.application.port.out;

import java.util.UUID;

public record OrderItemLineStatusUpdate(UUID orderItemId, String lineStatus) {
}
