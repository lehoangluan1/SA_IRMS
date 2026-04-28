package SA.irms.inventory.application.command;

import java.math.BigDecimal;

public final class InventoryCommands {
    private InventoryCommands() {}

    public record InventoryUpsert(String name, String unit, BigDecimal current, BigDecimal minimum, BigDecimal maximum,
                                  BigDecimal cost, String category) {}
}
