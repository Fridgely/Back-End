package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.food.service.FoodManager;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
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
        refrigeratorAccessValidator.validateMembership(addCategory.toKey());
        categoryAppender.appendCustomCategory(addCategory);
    }

    public CategoryDetailResponse findCategory(long categoryId, MemberRefrigeratorKey key) {
        refrigeratorAccessValidator.validateMembership(key);
        return CategoryDetailResponse.from(categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId()));
    }

    public List<CategoryResponse> findAllCategory(MemberRefrigeratorKey key) {
        refrigeratorAccessValidator.validateMembership(key);
        return CategoryResponse.from(categoryFinder.findAll(key.refrigeratorId()));
    }

    public void modifyCustomCategory(ModifyCategory modifyCategory) {
        refrigeratorAccessValidator.validateMembership(modifyCategory.toKey());
        categoryModifier.modify(modifyCategory);
    }

    /*
     * 삭제 대상 카테고리에 속한 모든 음식을 '기타' 카테고리로 이동한 후 대상 카테고리를 삭제
     */
    @Transactional
    public void removeCustomCategory(DeleteCategory deleteCategory) {
        refrigeratorAccessValidator.validateMembership(deleteCategory.toKey());
        foodManager.moveAllFoodsToFallback(deleteCategory.refrigeratorId(), deleteCategory.categoryId());
        categoryRemover.remove(deleteCategory);
    }

}