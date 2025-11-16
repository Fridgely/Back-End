package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.service.FoodManager;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryAppender categoryAppender;

    @Mock
    private CategoryFinder categoryFinder;

    @Mock
    private CategoryModifier categoryModifier;

    @Mock
    private RefrigeratorAccessValidator refrigeratorAccessValidator;

    @Mock
    private CategoryRemover categoryRemover;

    @Mock
    private FoodManager foodManager;

    @Test
    void 커스텀_카테고리를_생성한다() {
        // given
        var addCategoryDto = new AddCategory("customCategory", 1L, 1L);

        // when
        categoryService.appendCustomCategory(addCategoryDto);

        // then
        InOrder inOrder = inOrder(refrigeratorAccessValidator, categoryAppender);

        then(refrigeratorAccessValidator)
            .should(inOrder)
            .validateMembership(addCategoryDto.toKey());

        then(categoryAppender)
            .should(inOrder)
            .appendCustomCategory(addCategoryDto);
    }

    @Test
    void 냉장고에_속한_특정_카테고리를_조회한다() {
        // given
        long categoryId = 1L;
        long refrigeratorId = 1L;
        long memberId = 1L;

        Category mockCategory = mock(Category.class);
        given(categoryFinder.findByRefrigerator(categoryId, refrigeratorId))
            .willReturn(mockCategory);

        MemberRefrigeratorKey key = new MemberRefrigeratorKey(memberId, refrigeratorId);

        // when
        CategoryDetailResponse response = categoryService.findCategory(categoryId, key);

        // then
        then(refrigeratorAccessValidator)
            .should()
            .validateMembership(key);

        then(categoryFinder)
            .should()
            .findByRefrigerator(categoryId, refrigeratorId);
    }

    @Test
    void 냉장고에_속한_모든_카테고리를_조회한다() {
        // given
        long refrigeratorId = 1L;
        long memberId = 1L;
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(memberId, refrigeratorId);

        // when
        categoryService.findAllCategory(key);

        // then
        then(refrigeratorAccessValidator)
            .should()
            .validateMembership(key);

        then(categoryFinder)
            .should()
            .findAll(refrigeratorId);
    }

    @Test
    void 커스텀_카테고리의_이름을_수정한다() {
        // given
        var modifyCategoryDto = new ModifyCategory("modifiedCategory", 1L, 1L, 1L);

        // when
        categoryService.modifyCustomCategory(modifyCategoryDto);

        // then
        InOrder inOrder = inOrder(refrigeratorAccessValidator, categoryModifier);

        then(refrigeratorAccessValidator)
            .should(inOrder)
            .validateMembership(modifyCategoryDto.toKey());

        then(categoryModifier)
            .should(inOrder)
            .modify(modifyCategoryDto);
    }

    @Test
    void 커스텀_카테고리를_삭제하고_음식을_기본_카테고리로_이동한다() {
        // given
        var deleteCategory = new DeleteCategory(1L, 1L, 1L);

        // when
        categoryService.removeCustomCategory(deleteCategory);

        // then
        InOrder inOrder = inOrder(refrigeratorAccessValidator, foodManager, categoryRemover);

        then(refrigeratorAccessValidator)
            .should(inOrder)
            .validateMembership(deleteCategory.toKey());

        then(foodManager)
            .should(inOrder)
            .moveAllFoodsToFallback(deleteCategory.refrigeratorId(), deleteCategory.categoryId());

        then(categoryRemover)
            .should(inOrder)
            .remove(deleteCategory);
    }

}