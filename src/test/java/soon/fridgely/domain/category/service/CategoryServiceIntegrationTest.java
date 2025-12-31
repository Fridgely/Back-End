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
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.fixture.CategoryFixture;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

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
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member).sample()
        );
    }

    @Test
    void 커스텀_카테고리를_삭제하고_연결된_음식을_기본_카테고리로_이동한다() {
        // given
        Category customCategory = categoryRepository.save(
            CategoryFixture.category(fixtureMonkey, refrigerator, member)
                .set("name", "category")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category fallbackCategory = categoryRepository.save(
            CategoryFixture.category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        createFoods(3, customCategory);

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), customCategory.getId());

        // when
        categoryService.removeCustomCategory(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(customCategory.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();

        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods).hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
    }

    @Test
    void 권한이_없는_냉장고의_카테고리를_삭제하려_하면_예외가_발생한다() {
        // given
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey)
                .set("name", "남의 냉장고")
                .sample()
        );

        Category category = categoryRepository.save(
            category(fixtureMonkey, otherRefrigerator, otherMember).sample()
        );

        var deleteCategory = new DeleteCategory(member.getId(), otherRefrigerator.getId(), category.getId());

        // expected
        assertThatThrownBy(() -> categoryService.removeCustomCategory(deleteCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHORIZATION_FAILED);
    }

    private void createFoods(int count, Category category) {
        List<Food> foods = IntStream.range(0, count)
            .mapToObj(i -> food(fixtureMonkey, refrigerator, member, category).sample())
            .toList();
        foodRepository.saveAll(foods);
    }

}