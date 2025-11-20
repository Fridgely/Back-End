package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void 커스텀_카테고리를_삭제하고_연결된_음식을_기본_카테고리로_이동한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category1 = Category.register("category", refrigerator, member, CategoryType.CUSTOM);
        Category category2 = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(category1, category2));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, category1, LocalDate.now()))
            .limit(3)
            .toList();
        foodRepository.saveAll(foods);

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), category1.getId());

        // when
        categoryService.removeCustomCategory(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(category1.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();

        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods).hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId())
                .isEqualTo(category2.getId())
            );
    }

    @Test
    void 권한이_없는_냉장고의_카테고리를_삭제하려_하면_예외가_발생한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator otherRefrigerator = Refrigerator.register("남의 냉장고");
        refrigeratorRepository.save(otherRefrigerator);

        Category category = Category.register("target", otherRefrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        var deleteCommand = new DeleteCategory(member.getId(), otherRefrigerator.getId(), category.getId());

        // expected
        assertThatThrownBy(() -> categoryService.removeCustomCategory(deleteCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHORIZATION_FAILED);
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, LocalDate now) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            now
        );
    }

}