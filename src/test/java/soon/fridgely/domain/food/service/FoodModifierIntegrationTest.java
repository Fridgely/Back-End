package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodModifierIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodModifier foodModifier;

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
            category(fixtureMonkey, refrigerator, member)
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
    }

    @Test
    void 카테고리의_모든_음식을_기본_카테고리로_이동한다() {
        // given
        Category fallbackCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        createFoods(category); // 기존 카테고리에 음식 3개 생성

        // when
        foodModifier.moveAllFoodsToFallback(refrigerator.getId(), category.getId());

        // then
        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(food -> assertThat(food.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
    }

    @Test
    void 음식_정보_수정_시_카테고리가_변경되지_않으면_기존_카테고리를_유지한다() {
        // given
        Food food = createFood();

        var updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "수정된 홈런볼")
            .set("description", "바나나맛")
            .sample();

        // when
        foodModifier.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            category.getId()
        );

        // then
        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood)
            .extracting("name", "description")
            .containsExactly("수정된 홈런볼", "바나나맛");

        assertThat(updatedFood.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void 음식_정보_수정_시_카테고리_ID가_다르면_카테고리를_변경한다() {
        // given
        Food food = createFood(); // oldCategory(기본 category)에 생성
        Category newCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );

        var updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "수정된 홈런볼")
            .sample();

        // when
        foodModifier.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            newCategory.getId()
        );

        // then
        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood.getCategory().getId()).isEqualTo(newCategory.getId());
    }

    @Test
    void 음식을_추가하면_변경된_재고가_반영된다() {
        // given
        Quantity initialQuantity = Quantity.register(new BigDecimal("10.0"), Unit.L);
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", initialQuantity)
                .sample()
        );

        // when
        Quantity addAmount = Quantity.register(new BigDecimal("2.5"), Unit.L);
        foodModifier.add(food.getId(), refrigerator.getId(), addAmount);

        // then
        Food savedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(savedFood.getQuantity().getAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(12.5));
    }

    @Test
    void 음식을_소비하면_변경된_재고가_반영된다() {
        // given
        Quantity initialQuantity = Quantity.register(new BigDecimal("5.00"), Unit.PIECE);
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", initialQuantity)
                .sample()
        );

        // when
        Quantity consumeAmount = Quantity.register(new BigDecimal("2.00"), Unit.PIECE);
        foodModifier.consume(food.getId(), refrigerator.getId(), consumeAmount);

        // then
        Food savedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(savedFood.getQuantity().getAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    void 음식_수정_시_이미지_URL이_변경되면_새로운_이미지로_교체된다() {
        // given
        String oldImageUrl = "https://s3.example.com/images/old-uuid-test.jpg";
        String newImageUrl = "https://s3.example.com/images/new-uuid-test.jpg";

        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", oldImageUrl)
                .sample()
        );

        var updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", newImageUrl)
            .sample();

        // when
        foodModifier.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            category.getId()
        );

        // then
        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood.getImageURL()).isEqualTo(newImageUrl);
    }

    @Test
    void 음식_수정_시_이미지_URL이_같으면_이미지가_유지된다() {
        // given
        String sameImageUrl = "https://s3.example.com/images/same-uuid-test.jpg";

        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", sameImageUrl)
                .sample()
        );

        var updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", sameImageUrl)
            .sample();

        // when
        foodModifier.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            category.getId()
        );

        // then
        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood.getImageURL()).isEqualTo(sameImageUrl);
    }

    private Food createFood() {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", "https://s3.example.com/images/uuid-test.jpg")
                .sample()
        );
    }

    private void createFoods(Category targetCategory) {
        List<Food> foods = IntStream.range(0, 3)
            .mapToObj(i -> food(fixtureMonkey, refrigerator, member, targetCategory).sample())
            .toList();
        foodRepository.saveAll(foods);
    }

}