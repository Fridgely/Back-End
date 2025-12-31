package soon.fridgely.domain.category.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

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
    private CategoryRemover categoryRemover;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 커스텀_카테고리를_생성한다() {
        // given
        var addCategory = fixtureMonkey.giveMeOne(AddCategory.class);

        // when
        categoryService.appendCustomCategory(addCategory);

        // then
        InOrder inOrder = inOrder(categoryAppender);
        then(categoryAppender)
            .should(inOrder)
            .appendCustomCategory(addCategory);
    }

    @Test
    void 냉장고에_속한_특정_카테고리를_조회한다() {
        // given
        long categoryId = 1L;
        long refrigeratorId = 1L;

        Category mockCategory = fixtureMonkey.giveMeOne(Category.class);
        given(categoryFinder.findByRefrigerator(categoryId, refrigeratorId))
            .willReturn(mockCategory);

        var key = fixtureMonkey.giveMeBuilder(MemberRefrigeratorKey.class)
            .set("refrigeratorId", refrigeratorId)
            .sample();

        // when
        categoryService.findCategory(categoryId, key);

        // then
        then(categoryFinder)
            .should()
            .findByRefrigerator(categoryId, refrigeratorId);
    }

    @Test
    void 냉장고에_속한_모든_카테고리를_조회한다() {
        // given
        long refrigeratorId = 1L;
        var key = fixtureMonkey.giveMeBuilder(MemberRefrigeratorKey.class)
            .set("refrigeratorId", refrigeratorId)
            .sample();

        // when
        categoryService.findAllCategory(key);

        // then
        then(categoryFinder)
            .should()
            .findAll(refrigeratorId);
    }

    @Test
    void 커스텀_카테고리의_이름을_수정한다() {
        // given
        var modifyCategory = fixtureMonkey.giveMeOne(ModifyCategory.class);

        // when
        categoryService.modifyCustomCategory(modifyCategory);

        // then
        InOrder inOrder = inOrder(categoryModifier);
        then(categoryModifier)
            .should(inOrder)
            .modify(modifyCategory);
    }

    @Test
    void 커스텀_카테고리를_삭제한다() {
        // given
        var deleteCategory = fixtureMonkey.giveMeOne(DeleteCategory.class);

        // when
        categoryService.removeCustomCategory(deleteCategory);

        // then
        then(categoryRemover)
            .should()
            .remove(deleteCategory);
    }

}