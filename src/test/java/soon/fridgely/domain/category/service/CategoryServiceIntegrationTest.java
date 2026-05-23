package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
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
            member().sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator().sample()
        );
        memberRefrigeratorRepository.save(
            memberRefrigerator(refrigerator, member).sample()
        );
    }

    @Test
    void 냉장고_생성_시_기본_카테고리_8개가_생성된다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryService.appendDefaultCategories(key);

        // then
        List<Category> categories = categoryRepository.findAllByRefrigeratorAndStatus(refrigerator, EntityStatus.ACTIVE);
        assertThat(categories)
            .hasSize(8)
            .extracting("name", "type")
            .containsExactlyInAnyOrder(
                tuple("야채", CategoryType.DEFAULT),
                tuple("과일", CategoryType.DEFAULT),
                tuple("육류", CategoryType.DEFAULT),
                tuple("해산물", CategoryType.DEFAULT),
                tuple("유제품", CategoryType.DEFAULT),
                tuple("음료", CategoryType.DEFAULT),
                tuple("간식", CategoryType.DEFAULT),
                tuple("기타", CategoryType.DEFAULT)
            );
    }

    @Test
    void 기본_카테고리가_이미_존재하는_경우_중복_생성되지_않는다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryService.appendDefaultCategories(key);
        categoryService.appendDefaultCategories(key);

        // then
        List<Category> categories = categoryRepository.findAllByRefrigeratorAndStatus(refrigerator, EntityStatus.ACTIVE);
        assertThat(categories).hasSize(8);
    }

    @Test
    void 커스텀_카테고리를_추가한다() {
        // given
        var addCategory = fixtureMonkey.giveMeBuilder(AddCategory.class)
            .set("name", "새 카테고리")
            .set("refrigeratorId", refrigerator.getId())
            .set("memberId", member.getId())
            .sample();

        // when
        categoryService.appendCustomCategory(addCategory);

        // then
        boolean exists = categoryRepository.existsByNameAndRefrigeratorAndStatus(
            "새 카테고리", refrigerator, EntityStatus.ACTIVE
        );
        assertThat(exists).isTrue();
    }

    @Test
    void 동일한_냉장고에_중복된_이름의_카테고리를_추가하면_예외가_발생한다() {
        // given
        var addCommand = fixtureMonkey.giveMeBuilder(AddCategory.class)
            .set("name", "중복 카테고리")
            .set("refrigeratorId", refrigerator.getId())
            .set("memberId", member.getId())
            .sample();
        categoryService.appendCustomCategory(addCommand);

        // expected
        assertThatThrownBy(() -> categoryService.appendCustomCategory(addCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }

    @Test
    void 냉장고에_속한_모든_카테고리를_조회한다() {
        // given
        categoryRepository.saveAll(List.of(
            category(refrigerator, member).set("name", "카테고리1").sample(),
            category(refrigerator, member).set("name", "카테고리2").sample(),
            category(refrigerator, member).set("name", "카테고리3").sample()
        ));
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        List<CategoryResponse> result = categoryService.findAllCategory(key);

        // then
        assertThat(result).hasSize(3)
            .extracting(CategoryResponse::name)
            .containsExactlyInAnyOrder("카테고리1", "카테고리2", "카테고리3");
    }

    @Test
    void 커스텀_카테고리의_이름을_수정한다() {
        // given
        Category savedCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기존 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        var modifyCategory = createModifyCategory(savedCategory.getId(), "수정된 카테고리");

        // when
        categoryService.modifyCustomCategory(modifyCategory);

        // then
        Category modified = categoryRepository.findById(savedCategory.getId()).orElseThrow();
        assertThat(modified.getName()).isEqualTo("수정된 카테고리");
    }

    @Test
    void 기본_카테고리는_수정할_수_없다() {
        // given
        Category defaultCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기본 카테고리")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );
        var modifyCategory = createModifyCategory(defaultCategory.getId(), "수정 시도");

        // expected
        assertThatThrownBy(() -> categoryService.modifyCustomCategory(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.CANNOT_MODIFY_DEFAULT_CATEGORY);
    }

    @Test
    void 동일한_이름으로_수정하면_변경되지_않는다() {
        // given
        Category savedCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기존 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        var modifyCategory = createModifyCategory(savedCategory.getId(), "기존 카테고리");

        // when
        categoryService.modifyCustomCategory(modifyCategory);

        // then
        Category unchanged = categoryRepository.findById(savedCategory.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("기존 카테고리");
    }

    @Test
    void 이미_존재하는_카테고리_이름으로_수정하면_예외가_발생한다() {
        // given
        Category category1 = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기존1")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category category2 = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기존2")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        var modifyCategory = createModifyCategory(category1.getId(), category2.getName());

        // expected
        assertThatThrownBy(() -> categoryService.modifyCustomCategory(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }

    @Test
    void 존재하지_않는_카테고리를_수정하면_예외가_발생한다() {
        // given
        var modifyCommand = createModifyCategory(999L, "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryService.modifyCustomCategory(modifyCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 권한_없는_냉장고의_카테고리는_수정할_수_없다() {
        // given
        Member otherMember = memberRepository.save(member().sample());
        Refrigerator otherRefrigerator = refrigeratorRepository.save(refrigerator().sample());

        Category myCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "내 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );

        var modifyCommand = fixtureMonkey.giveMeBuilder(ModifyCategory.class)
            .set("newName", "수정 시도")
            .set("memberId", otherMember.getId())
            .set("refrigeratorId", otherRefrigerator.getId())
            .set("categoryId", myCategory.getId())
            .sample();

        // expected
        assertThatThrownBy(() -> categoryService.modifyCustomCategory(modifyCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHORIZATION_FAILED);
    }

    @Test
    void 커스텀_카테고리를_삭제하면_연결된_음식이_기타_카테고리로_이동한다() {
        // given
        Category customCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "삭제 대상")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category fallbackCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );
        createFoods(3, customCategory);

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), customCategory.getId());

        // when
        categoryService.removeCustomCategory(deleteCategory);

        // then
        Category deleted = categoryRepository.findById(customCategory.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();

        List<Food> movedFoods = foodRepository.findAll();
        assertThat(movedFoods).hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId()).isEqualTo(fallbackCategory.getId()));
    }

    @Test
    void 카테고리를_중복_삭제해도_예외가_발생하지_않는다() {
        // given
        Category targetCategory = categoryRepository.save(
            category(refrigerator, member)
                .set("name", "삭제 대상")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        categoryRepository.save(
            category(refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), targetCategory.getId());

        // when
        categoryService.removeCustomCategory(deleteCategory);
        categoryService.removeCustomCategory(deleteCategory);

        // then
        Category deleted = categoryRepository.findById(targetCategory.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    void 권한_없는_냉장고의_카테고리는_삭제할_수_없다() {
        // given
        Member otherMember = memberRepository.save(member().sample());
        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator().set("name", "남의 냉장고").sample()
        );
        Category otherCategory = categoryRepository.save(
            category(otherRefrigerator, otherMember).sample()
        );

        var deleteCategory = new DeleteCategory(member.getId(), otherRefrigerator.getId(), otherCategory.getId());

        // expected
        assertThatThrownBy(() -> categoryService.removeCustomCategory(deleteCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHORIZATION_FAILED);
    }

    private ModifyCategory createModifyCategory(Long categoryId, String newName) {
        return fixtureMonkey.giveMeBuilder(ModifyCategory.class)
            .set("newName", newName)
            .set("memberId", member.getId())
            .set("refrigeratorId", refrigerator.getId())
            .set("categoryId", categoryId)
            .sample();
    }

    private void createFoods(int count, Category category) {
        List<Food> foods = IntStream.range(0, count)
            .mapToObj(i -> food(refrigerator, member, category).sample())
            .toList();
        foodRepository.saveAll(foods);
    }
}
