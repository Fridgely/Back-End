package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.fixture.CategoryFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryRemoverIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryRemover categoryRemover;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodRepository foodRepository;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
    }

    @Test
    void 카테고리_삭제_시_음식을_이동시키고_카테고리를_삭제한다() {
        // given
        Category targetCategory = categoryRepository.save(
            CategoryFixture.category(fixtureMonkey, refrigerator, member)
                .set("name", "target")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category fallbackCategory = categoryRepository.save(
            CategoryFixture.category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        Food food = createFood(targetCategory);

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), targetCategory.getId());

        // when
        categoryRemover.remove(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(targetCategory.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();

        Food movedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(movedFood.getCategory().getId()).isEqualTo(fallbackCategory.getId());
    }

    @Test
    void 카테고리를_중복_삭제해도_예외가_발생하지_않는다() {
        // given
        Category targetCategory = categoryRepository.save(
            CategoryFixture.category(fixtureMonkey, refrigerator, member)
                .set("name", "target")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), targetCategory.getId());

        // when
        categoryRemover.remove(deleteCategory);
        categoryRemover.remove(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(targetCategory.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();
    }

    private Food createFood(Category category) {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category).sample()
        );
    }

}