package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.notification.dto.command.NotificationMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class NotificationMessageGeneratorUnitTest {

    private final NotificationMessageGenerator generator = new NotificationMessageGenerator();

    @Test
    void 음식이_1개일_때는_해당_음식의_이름으로_메시지를_생성한다() {
        // given
        Food food = mock(Food.class);
        given(food.getName()).willReturn("사과");

        List<Food> foods = List.of(food);
        int days = 3;

        // when
        var message = generator.generateForExpiredFoods(foods, days);

        // then
        assertThat(message.title()).isEqualTo("유통기한 임박 알림 ⏰");
        assertThat(message.body()).isEqualTo("'사과'의 소비기한이 3일 남았습니다.");
    }

    @Test
    void 음식이_2개_이상일_때는_다른_형식으로_메시지를_생성한다() {
        // given
        Food food1 = mock(Food.class);
        Food food2 = mock(Food.class);
        Food food3 = mock(Food.class);
        given(food1.getName()).willReturn("우유");

        List<Food> foods = List.of(food1, food2, food3);
        int days = 1;

        // when
        var message = generator.generateForExpiredFoods(foods, days);

        // then
        assertThat(message.title()).isEqualTo("유통기한 임박 알림 ⏰");
        assertThat(message.body()).isEqualTo("'우유' 외 2개 품목의 소비기한이 1일 남았습니다.");
    }

    @Test
    void 단건_재고_소진_알림_메시지를_생선한다() {
        // given
        Food food = mock(Food.class);
        given(food.getName()).willReturn("우유");
        List<Food> foods = List.of(food);

        // when
        NotificationMessage message = generator.generateForOutOfStockSummary(foods);

        // then
        assertThat(message.title()).isEqualTo("재고 소진 알림 ⏰");
        assertThat(message.body()).isEqualTo("우유 재고가 모두 소진되었습니다.");
    }

    @Test
    void 다건_재고_소진_알림_메시지를_생성한다() {
        // given
        Food food1 = mock(Food.class);
        Food food2 = mock(Food.class);
        Food food3 = mock(Food.class);
        given(food1.getName()).willReturn("계란");

        List<Food> foods = List.of(food1, food2, food3);

        // when
        NotificationMessage message = generator.generateForOutOfStockSummary(foods);

        // then
        assertThat(message.title()).isEqualTo("재고 소진 알림 ⏰");
        assertThat(message.body()).isEqualTo("계란 외 2건의 재고가 없습니다.");
    }

}