package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryAppenderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryAppender categoryAppender;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    void 냉장고ID와_멤버ID를_받아_기본_카테고리들을_일괄_생성한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryAppender.appendDefaultCategories(key);

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
        categoryAppender.appendDefaultCategories(key); // 최초 생성
        categoryAppender.appendDefaultCategories(key); // 중복 생성 시도

        // then
        List<Category> categories = categoryRepository.findAllByRefrigeratorAndStatus(refrigerator, EntityStatus.ACTIVE);
        assertThat(categories).hasSize(8);
    }

    @Test
    void 커스텀_카테고리를_추가한다() {
        // given
        var newCategory = fixtureMonkey.giveMeBuilder(AddCategory.class)
            .set("name", "newCategory")
            .set("refrigeratorId", refrigerator.getId())
            .set("memberId", member.getId())
            .sample();

        // when
        categoryAppender.appendCustomCategory(newCategory);

        // then
        boolean exists = categoryRepository.existsByNameAndRefrigeratorAndStatus("newCategory", refrigerator, EntityStatus.ACTIVE);
        assertThat(exists).isTrue();
    }

    @Test
    void 동일한_냉장고에_중복된_이름의_카테고리를_추가하면_예외가_발생한다() {
        // given
        var categoryName = "duplicateCategory";
        var addCommand = fixtureMonkey.giveMeBuilder(AddCategory.class)
            .set("name", categoryName)
            .set("refrigeratorId", refrigerator.getId())
            .set("memberId", member.getId())
            .sample();

        // when
        categoryAppender.appendCustomCategory(addCommand);

        // expected
        assertThatThrownBy(() -> categoryAppender.appendCustomCategory(addCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }

}