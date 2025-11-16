package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class FoodManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodManager foodManager;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 카테고리의_모든_음식을_기본_카테고리로_이동한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, targetCategory))
            .limit(3)
            .toList();
        foodRepository.saveAll(foods);

        // when
        foodManager.moveAllFoodsToFallback(refrigerator.getId(), targetCategory.getId());

        // then
        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(food -> assertThat(food.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
    }

    @Test
    void 음식을_등록한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        FoodInfo foodInfo = new FoodInfo(
            "홈런볼",
            category.getId(),
            new Quantity(BigDecimal.ONE, Unit.KG),
            LocalDateTime.now().plusDays(5L),
            StorageType.ROOM_TEMPERATURE,
            FoodStatus.GREEN,
            "초코맛",
            "http://example.com/image.jpg"
        );

        // when
        foodManager.createFood(foodInfo, new MemberRefrigeratorKey(member.getId(), refrigerator.getId()));

        // then
        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1)
            .extracting("name", "description")
            .containsExactly(tuple("홈런볼", "초코맛"));
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            FoodStatus.GREEN,
            "testDescription",
            "http://example.com/image.jpg"
        );
    }

}