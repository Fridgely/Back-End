package soon.fridgely.domain.food.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@ExtendWith(MockitoExtension.class)
class FoodRemoverUnitTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FoodRemover foodRemover;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member mockMember;
    private Refrigerator mockRefrigerator;
    private Category mockCategory;

    @BeforeEach
    void setUp() {
        mockMember = member(fixtureMonkey).sample();
        mockRefrigerator = refrigerator(fixtureMonkey).sample();
        mockCategory = category(fixtureMonkey, mockRefrigerator, mockMember).sample();
    }

    @Test
    void 음식_삭제_시_이미지_URL이_있으면_이벤트가_발행된다() {
        // given
        Food mockFood = food(fixtureMonkey, mockRefrigerator, mockMember, mockCategory)
            .set("imageURL", "https://s3.example.com/images/food.jpg")
            .sample();

        given(foodRepository.findByIdAndRefrigeratorId(anyLong(), anyLong()))
            .willReturn(Optional.of(mockFood));

        // when
        foodRemover.remove(1L, 1L);

        // then
        then(eventPublisher).should(times(1))
            .publishEvent(any(ImageDeleteEvent.class));
    }

    @Test
    void 음식_삭제_시_이미지_URL이_null이면_이벤트가_발행되지_않는다() {
        // given
        Food mockFood = food(fixtureMonkey, mockRefrigerator, mockMember, mockCategory)
            .set("imageURL", null)
            .sample();

        given(foodRepository.findByIdAndRefrigeratorId(anyLong(), anyLong()))
            .willReturn(Optional.of(mockFood));

        // when
        foodRemover.remove(1L, 1L);

        // then
        then(eventPublisher).should(never())
            .publishEvent(any(ImageDeleteEvent.class));
    }

}