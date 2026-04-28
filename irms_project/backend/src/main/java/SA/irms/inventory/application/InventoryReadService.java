package SA.irms.inventory.application;

import SA.irms.inventory.application.port.out.InventoryQueryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InventoryReadService {
    private final InventoryQueryRepository repository;

    public InventoryReadService(InventoryQueryRepository repository) {
        this.repository = repository;
    }

    public SA.irms.inventory.application.view.InventoryViews.InventoryOverview load() {
        return repository.load();
    }

    public SA.irms.inventory.application.view.InventoryViews.IngredientView findIngredient(UUID inventoryItemId) {
        return repository.findIngredient(inventoryItemId);
    }
}
