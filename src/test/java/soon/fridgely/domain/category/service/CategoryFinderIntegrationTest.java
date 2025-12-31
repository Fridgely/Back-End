package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryFinderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryFinder categoryFinder;

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
    void 냉장고ID와_카테고리ID로_카테고리를_조회한다() {
        // given
        Category category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "커스텀 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );

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
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        Category otherCategory = categoryRepository.save(
            category(fixtureMonkey, otherRefrigerator, otherMember).sample()
        );

        // expected
        assertThatThrownBy(() -> categoryFinder.findByRefrigerator(otherCategory.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 삭제된_카테고리는_조회되지_않는다() {
        // given
        Category category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "삭제될 카테고리")
                .set("type", CategoryType.CUSTOM)
                .set("status", EntityStatus.DELETED)
                .sample()
        );

        // expected
        assertThatThrownBy(() -> categoryFinder.findByRefrigerator(category.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 냉장고ID와_카테고리명으로_카테고리를_조회한다() {
        // given
        categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "타겟 카테고리")
                .set("type", CategoryType.CUSTOM)
                .sample()
        );

        // when
        Category foundCategory = categoryFinder.findByName("타겟 카테고리", refrigerator.getId());

        // then
        assertThat(foundCategory)
            .extracting("name", "type")
            .containsExactly("타겟 카테고리", CategoryType.CUSTOM);
    }

    @Test
    void 냉장고에_속한_모든_카테고리를_조회한다() {
        // given
        categoryRepository.saveAll(List.of(
            category(fixtureMonkey, refrigerator, member).set("name", "카테고리1").sample(),
            category(fixtureMonkey, refrigerator, member).set("name", "카테고리2").sample(),
            category(fixtureMonkey, refrigerator, member).set("name", "카테고리3").sample()
        ));

        // when
        List<Category> categories = categoryFinder.findAll(refrigerator.getId());

        // then
        assertThat(categories).hasSize(3)
            .extracting("name")
            .containsExactlyInAnyOrder("카테고리1", "카테고리2", "카테고리3");
    }

}