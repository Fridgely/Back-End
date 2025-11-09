package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.dto.ModifyCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryModifierIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryModifier categoryModifier;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 커스텀_카테고리의_이름을_수정한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("기존 카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        var modifyCategory = new ModifyCategory(refrigerator.getId(), member.getId(), category.getId(), "수정된 카테고리");

        // when
        categoryModifier.modify(modifyCategory);

        // then
        Category modifiedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(modifiedCategory.getName()).isEqualTo("수정된 카테고리");
    }

    @Test
    void 기본_카테고리는_수정할_수_없다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("기본 카테고리", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        Category defaultCategory = categoryRepository.findById(category.getId()).orElseThrow();

        var modifyCategory = new ModifyCategory(refrigerator.getId(), member.getId(), defaultCategory.getId(), "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.CANNOT_MODIFY_DEFAULT_CATEGORY);
    }

    @Test
    void 동일한_이름으로_수정하면_변경되지_않는다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("기존 카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        var modifyCategory = new ModifyCategory(refrigerator.getId(), member.getId(), category.getId(), "기존 카테고리");

        // when
        categoryModifier.modify(modifyCategory);

        // then
        Category unchangedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(unchangedCategory.getName()).isEqualTo(category.getName());
    }

    @Test
    void 이미_존재하는_카테고리_이름으로_수정하면_예외가_발생한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category1 = Category.register("기존 카테고리1", refrigerator, member, CategoryType.CUSTOM);
        Category category2 = Category.register("기존 카테고리2", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.saveAll(List.of(category1, category2));

        var modifyCategory = new ModifyCategory(refrigerator.getId(), member.getId(), category1.getId(), category2.getName());

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }

    @Test
    void 존재하지_않는_카테고리를_수정하면_예외가_발생한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        long nonExistentCategoryId = 999L;
        var modifyCategory = new ModifyCategory(refrigerator.getId(), member.getId(), nonExistentCategoryId, "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 다른_냉장고의_카테고리는_수정할_수_없다() {
        // given
        Member member1 = createMember("testId");
        Member member2 = createMember("testId2");
        memberRepository.saveAll(List.of(member1, member2));

        Refrigerator refrigerator1 = Refrigerator.register(member1.getNickname());
        Refrigerator refrigerator2 = Refrigerator.register(member2.getNickname());
        refrigeratorRepository.saveAll(List.of(refrigerator1, refrigerator2));

        Category category = Category.register("기존 카테고리", refrigerator1, member1, CategoryType.CUSTOM);
        categoryRepository.save(category);

        var modifyCategory = new ModifyCategory(refrigerator2.getId(), member2.getId(), category.getId(), "수정된 카테고리");

        // expected
        assertThatThrownBy(() -> categoryModifier.modify(modifyCategory))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    private Member createMember(String testId) {
        return Member.builder()
            .loginId(testId)
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}