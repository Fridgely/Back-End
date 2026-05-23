package soon.fridgely.domain.food.dto.response;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.FoodFixture.expirationDayFor;
import static soon.fridgely.global.support.fixture.FoodFixture.food;

class FoodsByStatusUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();
    private final LocalDate today = LocalDate.of(2025, 6, 1);
    private final AtomicLong idSeq = new AtomicLong(1);

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @BeforeEach
    void setUp() {
        member = fixtureMonkey.giveMeOne(Member.class);
        refrigerator = fixtureMonkey.giveMeOne(Refrigerator.class);
        category = fixtureMonkey.giveMeOne(Category.class);
        idSeq.set(1);
    }

    private ArbitraryBuilder<Food> foodOf(FoodStatus status) {
        return food(refrigerator, member, category, expirationDayFor(status, today), status)
            .set("id", idSeq.getAndIncrement());
    }

    @Test
    void 각_상태별_음식이_올바르게_분류된다() {
        // given
        List<Food> foods = List.of(
            foodOf(FoodStatus.BLACK).sample(),
            foodOf(FoodStatus.RED).sample(),
            foodOf(FoodStatus.YELLOW).sample(),
            foodOf(FoodStatus.GREEN).sample()
        );

        // when
        FoodsByStatus result = FoodsByStatus.of(foods, today);

        // then
        assertThat(FoodStatus.values())
            .extracting(status -> result.get(status).size())
            .containsExactly(1, 1, 1, 1);
    }

    @Test
    void 해당_상태에_음식이_없으면_빈_리스트를_반환한다() {
        // given
        Food greenFood = foodOf(FoodStatus.GREEN).sample();

        // when
        FoodsByStatus result = FoodsByStatus.of(List.of(greenFood), today);

        // then
        assertThat(List.of(FoodStatus.BLACK, FoodStatus.RED, FoodStatus.YELLOW))
            .extracting(status -> result.get(status).size())
            .containsOnly(0);
    }

    @Test
    void 각_상태의_목록과_개수가_일치한다() {
        // given
        List<Food> foods = List.of(
            foodOf(FoodStatus.BLACK).sample(),
            foodOf(FoodStatus.BLACK).sample(),
            foodOf(FoodStatus.RED).sample(),
            foodOf(FoodStatus.RED).sample(),
            foodOf(FoodStatus.RED).sample(),
            foodOf(FoodStatus.YELLOW).sample(),
            foodOf(FoodStatus.GREEN).sample(),
            foodOf(FoodStatus.GREEN).sample(),
            foodOf(FoodStatus.GREEN).sample(),
            foodOf(FoodStatus.GREEN).sample()
        );

        // when
        FoodStatusResponse response = FoodsByStatus.of(foods, today).toStatusResponse();

        // then
        assertThat(response)
            .extracting(
                r -> r.black().size(), FoodStatusResponse::blackCount,
                r -> r.red().size(), FoodStatusResponse::redCount,
                r -> r.yellow().size(), FoodStatusResponse::yellowCount,
                r -> r.green().size(), FoodStatusResponse::greenCount
            )
            .containsExactly(2, 2, 3, 3, 1, 1, 4, 4);
    }

    @Test
    void 음식이_없으면_모든_상태가_빈_리스트와_0개로_반환된다() {
        // when
        FoodStatusResponse response = FoodsByStatus.of(List.of(), today).toStatusResponse();

        // then
        assertThat(response)
            .extracting(
                r -> r.black().size(), FoodStatusResponse::blackCount,
                r -> r.red().size(), FoodStatusResponse::redCount,
                r -> r.yellow().size(), FoodStatusResponse::yellowCount,
                r -> r.green().size(), FoodStatusResponse::greenCount
            )
            .containsExactly(0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void get이_반환한_리스트를_수정해도_내부_상태가_변경되지_않는다() {
        // given
        Food greenFood = foodOf(FoodStatus.GREEN).sample();
        FoodsByStatus result = FoodsByStatus.of(List.of(greenFood), today);

        // expected
        assertThatThrownBy(() -> result.get(FoodStatus.GREEN).clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
