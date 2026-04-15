package soon.fridgely.domain.food.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@ExtendWith(MockitoExtension.class)
class FoodModifierUnitTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private CategoryFinder categoryFinder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FoodModifier foodModifier;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member mockMember;
    private Refrigerator mockRefrigerator;
    private Category mockCategory;
    private MemberRefrigeratorKey key;

    @BeforeEach
    void setUp() {
        mockMember = member(fixtureMonkey).sample();
        mockRefrigerator = refrigerator(fixtureMonkey).sample();
        mockCategory = category(fixtureMonkey, mockRefrigerator, mockMember)
            .set("id", 1L)
            .sample();

        key = new MemberRefrigeratorKey(1L, 1L);
    }

    @Test
    void 이미지_URL이_변경되면_이벤트가_발행된다() {
        // given
        String oldImageUrl = "https://s3.example.com/images/old-image.jpg";
        String newImageUrl = "https://s3.example.com/images/new-image.jpg";

        Food mockFood = food(fixtureMonkey, mockRefrigerator, mockMember, mockCategory)
            .set("imageURL", oldImageUrl)
            .sample();

        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", newImageUrl)
            .sample();

        given(foodRepository.findByIdAndRefrigeratorIdAndStatus(anyLong(), anyLong(), eq(EntityStatus.ACTIVE)))
            .willReturn(Optional.of(mockFood));

        // when
        foodModifier.update(1L, updateInfo, key, 1L);

        // then
        then(eventPublisher).should(times(1))
            .publishEvent(any(ImageDeleteEvent.class));
    }

    @Test
    void 기존_이미지가_null이고_새_이미지가_있으면_이벤트가_발행되지_않는다() {
        // given
        Food mockFood = food(fixtureMonkey, mockRefrigerator, mockMember, mockCategory)
            .set("imageURL", null)
            .sample();

        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", "https://s3.example.com/images/new-image.jpg")
            .sample();

        given(foodRepository.findByIdAndRefrigeratorIdAndStatus(anyLong(), anyLong(), eq(EntityStatus.ACTIVE)))
            .willReturn(Optional.of(mockFood));

        // when
        foodModifier.update(1L, updateInfo, key, 1L);

        // then
        then(eventPublisher).should(never())
            .publishEvent(any(ImageDeleteEvent.class));
    }

    @Test
    void 카테고리_ID가_동일하면_카테고리를_조회하지_않는다() {
        // given
        Food mockFood = food(fixtureMonkey, mockRefrigerator, mockMember, mockCategory)
            .set("category", mockCategory)
            .sample();

        // imageURL을 null로 설정하여 이벤트 발행 분기를 제거
        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .setNull("imageURL")
            .sample();

        given(foodRepository.findByIdAndRefrigeratorIdAndStatus(anyLong(), anyLong(), eq(EntityStatus.ACTIVE)))
            .willReturn(Optional.of(mockFood));

        // when
        foodModifier.update(1L, updateInfo, key, mockCategory.getId());

        // then
        then(categoryFinder).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_음식을_수정하면_예외가_발생한다() {
        // given
        given(foodRepository.findByIdAndRefrigeratorIdAndStatus(anyLong(), anyLong(), eq(EntityStatus.ACTIVE)))
            .willReturn(Optional.empty());

        FoodInfo updateInfo = fixtureMonkey.giveMeOne(FoodInfo.class);

        // expected
        assertThatThrownBy(() -> foodModifier.update(999L, updateInfo, key, 1L))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

}