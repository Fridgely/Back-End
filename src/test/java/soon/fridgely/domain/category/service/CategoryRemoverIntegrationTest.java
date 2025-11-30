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
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void 카테고리_삭제_시_음식을_이동시키고_카테고리를_삭제한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("target", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        Food food = createFood(refrigerator, member, targetCategory, LocalDate.now());
        foodRepository.save(food);

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
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("target", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), targetCategory.getId());

        // when
        categoryRemover.remove(deleteCategory);
        categoryRemover.remove(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(targetCategory.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();
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