package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

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

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member).sample()
        );
        this.category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );
    }

    @Test
    void 음식을_조회한다() {
        // given
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "testFood")
                .set("description", "testDescription")
                .sample()
        );

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
        createFood(FoodStatus.RED);
        createFood(FoodStatus.GREEN);
        createFood(FoodStatus.GREEN);
        createFood(FoodStatus.BLACK);

        // when
        List<Food> foods = foodFinder.findAllMyFoods(member.getId());

        // then
        assertThat(foods).hasSize(4)
            .extracting("foodStatus")
            .containsExactlyInAnyOrder(
                FoodStatus.RED,
                FoodStatus.GREEN,
                FoodStatus.GREEN,
                FoodStatus.BLACK
            );
    }

    private void createFood(FoodStatus status) {
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("foodStatus", status)
                .sample()
        );
    }

}