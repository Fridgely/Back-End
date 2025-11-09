package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.category.dto.AddCategory;
import soon.fridgely.domain.category.dto.ModifyCategory;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryAppender categoryAppender;

    @Mock
    private CategoryModifier categoryModifier;

    @Mock
    private RefrigeratorAccessValidator refrigeratorAccessValidator;

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
            .validateMembership(addCategoryDto.refrigeratorId(), addCategoryDto.memberId());

        then(categoryAppender)
            .should(inOrder)
            .appendCustomCategory(addCategoryDto);
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
            .validateMembership(modifyCategoryDto.refrigeratorId(), modifyCategoryDto.memberId());

        then(categoryModifier)
            .should(inOrder)
            .modify(modifyCategoryDto);
    }

}