package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import SA.irms.common.api.ApiErrorResponse;
import SA.irms.common.error.ValidationException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;

@Service
class MenuRecipeService {
    private static final Pattern INGREDIENT_PATTERN = Pattern.compile("(.+?):\\s*(\\d+(?:\\.\\d+)?)");

    private final OrderRepository repository;
    private final OrderQueryRepository queryRepository;

    MenuRecipeService(OrderRepository repository, OrderQueryRepository queryRepository) {
        this.repository = repository;
        this.queryRepository = queryRepository;
    }

    void writeRecipe(UUID menuItemId, List<String> ingredientInputs, int version) {
        if (ingredientInputs == null || ingredientInputs.isEmpty()) {
            throw new ValidationException("Ingredient mappings are required for each dish.",
                    List.of(new ApiErrorResponse.FieldError("ingredients",
                            "At least one ingredient mapping is required in the format Ingredient:Quantity.")));
        }
        UUID recipeId = UUID.randomUUID();
        repository.createRecipe(recipeId, menuItemId, version);
        List<ApiErrorResponse.FieldError> errors = new ArrayList<>();
        for (String ingredientInput : ingredientInputs) {
            writeRecipeLine(recipeId, ingredientInput, errors);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Ingredient mappings could not be saved.", errors);
        }
    }

    List<String> loadIngredientDisplay(UUID menuItemId) {
        return queryRepository.loadIngredientDisplay(menuItemId);
    }

    private void writeRecipeLine(UUID recipeId, String ingredientInput, List<ApiErrorResponse.FieldError> errors) {
        Matcher matcher = INGREDIENT_PATTERN.matcher(ingredientInput);
        if (!matcher.matches()) {
            errors.add(new ApiErrorResponse.FieldError("ingredients", "Use the format Ingredient:Quantity for each ingredient entry."));
            return;
        }
        String ingredientName = matcher.group(1).trim();
        BigDecimal quantity = new BigDecimal(matcher.group(2));
        UUID inventoryItemId = repository.findInventoryItemIdByName(ingredientName).orElse(null);
        if (inventoryItemId == null) {
            errors.add(new ApiErrorResponse.FieldError("ingredients", "Ingredient '" + ingredientName + "' does not exist in inventory."));
            return;
        }
        repository.createRecipeLine(recipeId, inventoryItemId, quantity);
    }
}
