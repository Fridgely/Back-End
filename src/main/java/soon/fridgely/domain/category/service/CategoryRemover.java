package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.category.validator.CategoryValidator;
import soon.fridgely.domain.food.service.FoodModifier;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CategoryRemover {

    private final CategoryValidator categoryValidator;
    private final FoodModifier foodModifier;
    private final CategoryRepository categoryRepository;

    @CacheEvict(value = "categories", key = "#deleteCategory.refrigeratorId()")
    @Transactional
    public void remove(DeleteCategory deleteCategory) {
        Category category = categoryRepository.findByIdAndRefrigeratorId(deleteCategory.categoryId(), deleteCategory.refrigeratorId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        if (category.isDeleted()) {
            return;
        }

        categoryValidator.validateNotDefaultType(category);

        foodModifier.moveAllFoodsToFallback(deleteCategory.refrigeratorId(), category.getId());

        category.delete();
        categoryRepository.save(category); // foodModifier에서 영속성 컨텍스트가 초기화되므로 다시 저장 필요
    }

    @CacheEvict(value = "categories", key = "#refrigeratorId")
    @Transactional
    public void removeAllByRefrigeratorId(long refrigeratorId) {
        List<Category> categories = categoryRepository.findAllByRefrigeratorIdAndStatus(refrigeratorId, EntityStatus.ACTIVE);
        categories.forEach(Category::delete);
    }

}