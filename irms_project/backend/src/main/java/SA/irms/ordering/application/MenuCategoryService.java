package SA.irms.ordering.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderRepository;

@Service
class MenuCategoryService {
    private final OrderRepository repository;
    private final MenuReadService menuReadService;

    MenuCategoryService(OrderRepository repository, MenuReadService menuReadService) {
        this.repository = repository;
        this.menuReadService = menuReadService;
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.CategoryView createCategory(String name) {
        UUID categoryId = UUID.randomUUID();
        repository.createCategory(categoryId, name, repository.nextCategoryDisplayOrder());
        return new SA.irms.ordering.application.view.MenuViews.CategoryView(categoryId, name, 0, true);
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.CategoryView updateCategory(UUID categoryId, String name) {
        if (!repository.updateCategory(categoryId, name)) {
            throw new NotFoundException("Category was not found.");
        }
        return menuReadService.findCategory(categoryId);
    }

    @Transactional
    void deleteCategory(UUID categoryId) {
        if (repository.countMenuItemsInCategory(categoryId) > 0) {
            throw new ConflictException("The category still contains menu items.");
        }
        if (!repository.deleteCategory(categoryId)) {
            throw new NotFoundException("Category was not found.");
        }
    }
}
