package SA.irms.inventory.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.inventory.application.port.out.InventoryIngredientRepository;
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.context.RequestMetadata;

@Service
public class InventoryIngredientService {
    private final InventoryIngredientRepository repository;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final InventoryStockService inventoryStockService;
    private final InventoryLowStockAlertService lowStockAlertService;
    private final InventoryReadService inventoryReadService;

    public InventoryIngredientService(
            InventoryIngredientRepository repository,
            SharedIdentityPolicyPort identityPolicyPort,
            AuditRecorder auditService,
            InventoryStockService inventoryStockService,
            InventoryReadService inventoryReadService,
            InventoryLowStockAlertService lowStockAlertService) {
        this.repository = repository;
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.inventoryStockService = inventoryStockService;
        this.inventoryReadService = inventoryReadService;
        this.lowStockAlertService = lowStockAlertService;
    }

    @Transactional
    public SA.irms.inventory.application.view.InventoryViews.IngredientView createIngredient(
            SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert request) {
        UUID inventoryItemId = UUID.randomUUID();
        BranchView branch = identityPolicyPort.findDefaultBranch();
        repository.createIngredient(inventoryItemId, branch.branchId(), request.name(), request.unit(),
                request.current(),
                request.minimum(), request.maximum(), request.cost(), request.category());
        repository.upsertReorderRule(inventoryItemId, request.minimum(), request.maximum());
        return inventoryReadService.findIngredient(inventoryItemId);
    }

    @Transactional
    public SA.irms.inventory.application.view.InventoryViews.IngredientView updateIngredient(UUID inventoryItemId,
            SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert request,
            UUID actorUserId, String correlationId, RequestMetadata httpServletRequest) {
        SA.irms.inventory.application.view.InventoryViews.IngredientView existing = inventoryReadService
                .findIngredient(inventoryItemId);
        repository.updateIngredient(inventoryItemId, request.name(), request.unit(), request.current(),
                request.minimum(),
                request.maximum(), request.cost(), request.category());
        repository.upsertReorderRule(inventoryItemId, request.minimum(), request.maximum());
        lowStockAlertService.evaluateLowStock(
                inventoryItemId,
                correlationId);
        if (existing.current().compareTo(request.current()) != 0) {
            inventoryStockService.recordManualAdjustment(inventoryItemId,
                    request.current().subtract(existing.current()), actorUserId, correlationId);
            auditService.record(actorUserId, "inventory.manual_adjustment", "InventoryItem", inventoryItemId.toString(),
                    correlationId,
                    "Inventory record updated from the monitoring screen.", false, httpServletRequest.remoteIp(),
                    Map.of("onHand", existing.current()), Map.of("onHand", request.current()));
        }
        return inventoryReadService.findIngredient(inventoryItemId);
    }

    @Transactional
    public void deleteIngredient(UUID inventoryItemId) {
        long dependentRecipes = repository.countDependentRecipes(inventoryItemId);
        if (dependentRecipes > 0) {
            throw new ConflictException("The ingredient is still referenced by menu recipes.");
        }
        int deleted = repository.deleteIngredient(inventoryItemId);
        if (deleted == 0) {
            throw new NotFoundException("Ingredient was not found.");
        }
    }
}