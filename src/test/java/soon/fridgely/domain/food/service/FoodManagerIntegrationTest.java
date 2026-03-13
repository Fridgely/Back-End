package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodManager foodManager;

    @Autowired
    private FoodRemover foodRemover;

    @Autowired
    private FoodFinder foodFinder;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    void 음식을_등록한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        var foodInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "홈런볼")
            .set("description", "초코맛")
            .sample();

        // when
        foodManager.createFood(foodInfo, key, category.getId());

        // then
        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1)
            .extracting("name", "description")
            .containsExactly(tuple("홈런볼", "초코맛"));
    }

    @Test
    void 음식을_삭제하면_조회되지_않아야_한다() {
        // given
        Food food = createFood();

        // when
        foodRemover.remove(food.getId(), refrigerator.getId());

        // then
        assertThatThrownBy(() -> foodFinder.find(food.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);

        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 음식을_중복_삭제해도_예외가_발생하지_않는다() {
        // given
        Food food = createFood();

        // when
        foodRemover.remove(food.getId(), refrigerator.getId());
        foodRemover.remove(food.getId(), refrigerator.getId());

        // then
        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 음식_삭제_시_이미지_URL이_있으면_소프트_삭제된다() {
        // given
        String imageUrl = "https://s3.example.com/images/test-food.jpg";
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", imageUrl)
                .sample()
        );

        // when
        foodRemover.remove(food.getId(), refrigerator.getId());

        // then
        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood).isNotNull()
            .extracting("imageURL", "status")
            .containsExactly(imageUrl, EntityStatus.DELETED);
    }

    @Test
    void 음식_삭제_시_이미지_URL이_빈_문자열이면_정상_삭제된다() {
        // given
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", "")
                .sample()
        );

        // when
        foodRemover.remove(food.getId(), refrigerator.getId());

        // then
        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    private Food createFood() {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", "https://s3.example.com/images/uuid-test.jpg")
                .sample()
        );
    }

}