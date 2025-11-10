package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
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

class CategoryFinderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryFinder categoryFinder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 냉장고ID와_카테고리ID로_카테고리를_조회한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("커스텀 카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        // when
        Category foundCategory = categoryFinder.findByRefrigerator(category.getId(), refrigerator.getId());

        // then
        assertThat(foundCategory)
            .extracting("name", "type", "refrigerator.id", "member.id")
            .containsExactly("커스텀 카테고리", CategoryType.CUSTOM, refrigerator.getId(), member.getId());
    }

    @Test
    void 존재하지_않는_카테고리ID로_조회하면_예외가_발생한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        long nonExistentCategoryId = 999L;

        // expected
        assertThatThrownBy(() -> categoryFinder.findByRefrigerator(nonExistentCategoryId, refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 다른_냉장고의_카테고리를_조회하면_예외가_발생한다() {
        // given
        Member member1 = createMember("testId");
        Member member2 = createMember("testId2");
        memberRepository.saveAll(List.of(member1, member2));

        Refrigerator refrigerator1 = Refrigerator.register(member1.getNickname());
        Refrigerator refrigerator2 = Refrigerator.register(member2.getNickname());
        refrigeratorRepository.saveAll(List.of(refrigerator1, refrigerator2));

        Category category = Category.register("카테고리", refrigerator1, member1, CategoryType.CUSTOM);
        categoryRepository.save(category);

        // expected
        assertThatThrownBy(() -> categoryFinder.findByRefrigerator(category.getId(), refrigerator2.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 삭제된_카테고리는_조회되지_않는다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        category.delete();
        categoryRepository.save(category);

        // expected
        assertThatThrownBy(() -> categoryFinder.findByRefrigerator(category.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 냉장고ID와_카테고리명으로_카테고리를_조회한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        // when
        Category foundCategory = categoryFinder.findByName("카테고리", refrigerator.getId());

        // then
        assertThat(foundCategory)
            .extracting("name", "type", "refrigerator.id", "member.id")
            .containsExactly("카테고리", CategoryType.CUSTOM, refrigerator.getId(), member.getId());
    }

    @Test
    void 냉장고에_속한_모든_카테고리를_조회한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category1 = Category.register("카테고리1", refrigerator, member, CategoryType.CUSTOM);
        Category category2 = Category.register("카테고리2", refrigerator, member, CategoryType.CUSTOM);
        Category category3 = Category.register("카테고리3", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(category1, category2, category3));

        // when
        List<Category> categories = categoryFinder.findAll(refrigerator.getId());

        // then
        assertThat(categories).hasSize(3)
            .extracting("name")
            .containsExactlyInAnyOrder("카테고리1", "카테고리2", "카테고리3");
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