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
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.expirationDayFor;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodStatusSchedulerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodStatusScheduler foodStatusScheduler;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        this.category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );
    }

    @Test
    void 모든_status가_일괄_갱신된다() {
        // given
        LocalDate today = LocalDate.now();
        Food blackFood = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, expirationDayFor(FoodStatus.BLACK, today), FoodStatus.GREEN).sample()
        );
        Food redFood = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, expirationDayFor(FoodStatus.RED, today), FoodStatus.GREEN).sample()
        );
        Food yellowFood = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, expirationDayFor(FoodStatus.YELLOW, today), FoodStatus.GREEN).sample()
        );
        Food greenFood = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, expirationDayFor(FoodStatus.GREEN, today), FoodStatus.BLACK).sample()
        );

        // when
        foodStatusScheduler.updateFoodStatuses();

        // then
        List<Food> results = foodRepository.findAllById(
            List.of(blackFood.getId(), redFood.getId(), yellowFood.getId(), greenFood.getId())
        );
        assertThat(results).extracting(Food::getFoodStatus)
            .containsExactlyInAnyOrder(FoodStatus.BLACK, FoodStatus.RED, FoodStatus.YELLOW, FoodStatus.GREEN);
    }

    @Test
    void RED_YELLOW_경계값인_D플러스10은_RED이고_D플러스11은_YELLOW이다() {
        // given
        LocalDate today = LocalDate.now();
        Food red = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, today.plusDays(FoodStatus.RED.daysThreshold).atStartOfDay(), FoodStatus.GREEN).sample()
        );
        Food yellow = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category, today.plusDays(FoodStatus.RED.nextThresholdDay()).atStartOfDay(), FoodStatus.GREEN).sample()
        );

        // when
        foodStatusScheduler.updateFoodStatuses();

        // then
        Food updatedRed = foodRepository.findById(red.getId()).orElseThrow();
        assertThat(updatedRed.getFoodStatus()).isEqualTo(FoodStatus.RED);

        Food updatedYellow = foodRepository.findById(yellow.getId()).orElseThrow();
        assertThat(updatedYellow.getFoodStatus()).isEqualTo(FoodStatus.YELLOW);
    }

}