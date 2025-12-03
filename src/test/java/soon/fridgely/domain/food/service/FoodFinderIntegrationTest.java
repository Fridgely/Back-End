package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodFinderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodFinder foodFinder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Test
    void 음식을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now(), FoodStatus.GREEN);
        foodRepository.save(food);

        // when
        Food found = foodFinder.find(food.getId(), refrigerator.getId());

        // then
        assertThat(found).isNotNull()
            .extracting("name", "description")
            .containsExactly("testFood", "testDescription");
    }

    @Test
    void 사용자의_모든_음식을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food redFood = createFood(refrigerator, member, category, LocalDate.now(), FoodStatus.RED);
        Food greenFood1 = createFood(refrigerator, member, category, LocalDate.now(), FoodStatus.GREEN);
        Food greenFood2 = createFood(refrigerator, member, category, LocalDate.now(), FoodStatus.GREEN);
        Food blackFood = createFood(refrigerator, member, category, LocalDate.now(), FoodStatus.BLACK);
        foodRepository.saveAll(List.of(redFood, greenFood1, greenFood2, blackFood));

        // when
        List<Food> foods = foodFinder.findAllMyFoods(member.getId());

        // then
        assertThat(foods).hasSize(4)
            .extracting("foodStatus")
            .containsExactlyInAnyOrder(FoodStatus.RED, FoodStatus.GREEN, FoodStatus.GREEN, FoodStatus.BLACK);
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, LocalDate now, FoodStatus status) {
        return Food.builder()
            .refrigerator(refrigerator)
            .member(member)
            .name("testFood")
            .category(category)
            .quantity(new Quantity(new BigDecimal("1.0"), Unit.KG))
            .expirationDate(now.plusDays(2L).atStartOfDay())
            .storageType(StorageType.FROZEN)
            .description("testDescription")
            .imageURL("http://example.com/image.jpg")
            .foodStatus(status)
            .build();
    }

}