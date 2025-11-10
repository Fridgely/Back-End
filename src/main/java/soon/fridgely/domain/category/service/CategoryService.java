package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.dto.AddCategory;
import soon.fridgely.domain.category.dto.DeleteCategory;
import soon.fridgely.domain.category.dto.ModifyCategory;
import soon.fridgely.domain.category.service.dto.response.CategoryResponse;
import soon.fridgely.domain.food.service.FoodManager;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryAppender categoryAppender;
    private final CategoryFinder categoryFinder;
    private final CategoryModifier categoryModifier;
    private final CategoryRemover categoryRemover;
    private final FoodManager foodManager;

    private final RefrigeratorAccessValidator refrigeratorAccessValidator;

    public void appendCustomCategory(AddCategory addCategory) {
        refrigeratorAccessValidator.validateMembership(addCategory.refrigeratorId(), addCategory.memberId());
        categoryAppender.appendCustomCategory(addCategory);
    }

    public List<CategoryResponse> findAll(long refrigeratorId, long memberId) {
        refrigeratorAccessValidator.validateMembership(refrigeratorId, memberId);
        return CategoryResponse.from(categoryFinder.findAll(refrigeratorId));
    }

    public void modifyCustomCategory(ModifyCategory modifyCategory) {
        refrigeratorAccessValidator.validateMembership(modifyCategory.refrigeratorId(), modifyCategory.memberId());
        categoryModifier.modify(modifyCategory);
    }

    /*
     * 삭제 대상 카테고리에 속한 모든 음식을 '기타' 카테고리로 이동한 후 대상 카테고리를 삭제
     */
    @Transactional
    public void removeCustomCategory(DeleteCategory deleteCategory) {
        refrigeratorAccessValidator.validateMembership(deleteCategory.refrigeratorId(), deleteCategory.memberId());
        foodManager.moveAllFoodsToFallback(deleteCategory.refrigeratorId(), deleteCategory.categoryId());
        categoryRemover.remove(deleteCategory);
    }

}