package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryModifierIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryModifier categoryModifier;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

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
    void 커스텀_카테고리의_이름을_수정한다() {
        // given
        Category category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기존 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        var modifyCategory = createModifyCategory(category.getId(), "수정된 카테고리");

        // when
        categoryModifier.modify(modifyCategory);

        // then
        Category modifiedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(modifiedCategory.getName()).isEqualTo("수정된 카테고리");
    }

    @Test
    void 기본_카테고리는_수정할_수_없다() {
        // given
        Category category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기본 카테고리")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );
        var modifyCategory = createModifyCategory(category.getId(), "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.CANNOT_MODIFY_DEFAULT_CATEGORY);
    }

    @Test
    void 동일한_이름으로_수정하면_변경되지_않는다() {
        // given
        Category category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기존 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        var modifyCategory = createModifyCategory(category.getId(), "기존 카테고리"); // 같은 이름

        // when
        categoryModifier.modify(modifyCategory);

        // then
        Category unchangedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(unchangedCategory.getName()).isEqualTo("기존 카테고리");
    }

    @Test
    void 이미_존재하는_카테고리_이름으로_수정하면_예외가_발생한다() {
        // given
        Category category1 = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기존1")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category category2 = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기존2")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );

        // category1의 이름을 category2("기존2")로 변경 시도
        var modifyCategory = createModifyCategory(category1.getId(), category2.getName());

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }

    @Test
    void 존재하지_않는_카테고리를_수정하면_예외가_발생한다() {
        // given
        long nonExistentCategoryId = 999L;
        var modifyCommand = createModifyCategory(nonExistentCategoryId, "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 다른_냉장고의_카테고리는_수정할_수_없다() {
        // given
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        Category myCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
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
        assertThatThrownBy(() -> categoryModifier.modify(modifyCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    private ModifyCategory createModifyCategory(Long categoryId, String newName) {
        return fixtureMonkey.giveMeBuilder(ModifyCategory.class)
            .set("newName", newName)
            .set("memberId", member.getId())
            .set("refrigeratorId", refrigerator.getId())
            .set("categoryId", categoryId)
            .sample();
    }

}