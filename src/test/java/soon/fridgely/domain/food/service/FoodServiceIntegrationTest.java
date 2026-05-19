package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.dto.request.FoodCursorPageRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodListResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodService foodService;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Refrigerator refrigerator;
    private Category category;
    private MemberRefrigeratorKey key;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(member(fixtureMonkey).sample());
        this.refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(memberRefrigerator(fixtureMonkey, refrigerator, member).sample());
        this.category = categoryRepository.save(category(fixtureMonkey, refrigerator, member).sample());
        this.key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
    }

    @Test
    void 음식을_조회한다() {
        // given
        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "testFood")
                .set("description", "testDescription")
                .sample()
        );

        // when
        FoodDetailResponse response = foodService.findFood(saved.getId(), key);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("testFood");
    }

    @Test
    void 사용자의_모든_음식을_상태별로_그룹핑하여_조회한다() {
        // given
        createFoodWithStatus(FoodStatus.RED);
        createFoodWithStatus(FoodStatus.GREEN);
        createFoodWithStatus(FoodStatus.GREEN);
        createFoodWithStatus(FoodStatus.BLACK);

        // when
        FoodStatusResponse response = foodService.findAllMyFoodsGroupedByStatus(member.getId());

        // then
        assertThat(response.red()).hasSize(1);
        assertThat(response.green()).hasSize(2);
        assertThat(response.black()).hasSize(1);
    }

    @Test
    void 저장_위치로_필터링하여_음식을_조회한다() {
        // given
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉장음식")
                .set("storageType", StorageType.REFRIGERATION)
                .sample()
        );
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉동음식")
                .set("storageType", StorageType.FROZEN)
                .sample()
        );

        FoodCursorPageRequest request = new FoodCursorPageRequest(null, 10, null, StorageType.REFRIGERATION);

        // when
        Slice<FoodListResponse> result = foodService.findAllFoods(key, request);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("냉장음식");
    }

    @Test
    void 음식을_등록한다() {
        // given
        FoodInfo info = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "홈런볼")
            .set("description", "초코맛")
            .sample();

        // when
        foodService.createFood(info, key, category.getId());

        // then
        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1)
            .extracting("name", "description")
            .containsExactly(tuple("홈런볼", "초코맛"));
    }

    @Test
    void 음식을_삭제하면_조회되지_않아야_한다() {
        // given
        Food saved = createFood();

        // when
        foodService.deleteFood(saved.getId(), key);

        // then
        assertThatThrownBy(() -> foodService.findFood(saved.getId(), key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);

        Food deletedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 음식을_중복_삭제해도_예외가_발생하지_않는다() {
        // given
        Food saved = createFood();

        // when
        foodService.deleteFood(saved.getId(), key);
        foodService.deleteFood(saved.getId(), key);

        // then
        Food deletedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 음식_삭제_시_이미지_URL이_있으면_소프트_삭제된다() {
        // given
        String imageUrl = "https://s3.example.com/images/test-food.jpg";
        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", imageUrl)
                .sample()
        );

        // when
        foodService.deleteFood(saved.getId(), key);

        // then
        Food deletedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(deletedFood)
            .extracting("imageURL", "status")
            .containsExactly(imageUrl, EntityStatus.DELETED);
    }

    @Test
    void 음식_삭제_시_이미지_URL이_빈_문자열이면_정상_삭제된다() {
        // given
        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", "")
                .sample()
        );

        // when
        foodService.deleteFood(saved.getId(), key);

        // then
        Food deletedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 카테고리의_모든_음식을_기본_카테고리로_이동한다() {
        // given
        Category customCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
        Category fallbackCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );
        List<Food> foods = IntStream.range(0, 3)
            .mapToObj(i -> food(fixtureMonkey, refrigerator, member, customCategory).sample())
            .toList();
        foodRepository.saveAll(foods);

        // when
        foodService.moveAllFoodsToFallback(refrigerator.getId(), customCategory.getId());

        // then
        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId()).isEqualTo(fallbackCategory.getId()));
    }

    @Test
    void 음식_정보_수정_시_카테고리가_변경되지_않으면_기존_카테고리를_유지한다() {
        // given
        Food saved = createFood();
        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "수정된 홈런볼")
            .set("description", "바나나맛")
            .setNull("imageURL")
            .sample();

        // when
        foodService.updateFood(saved.getId(), updateInfo, key, category.getId());

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood)
            .extracting("name", "description")
            .containsExactly("수정된 홈런볼", "바나나맛");
        assertThat(updatedFood.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void 음식_정보_수정_시_카테고리_ID가_다르면_카테고리를_변경한다() {
        // given
        Food saved = createFood();
        Category newCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );
        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("name", "수정된 홈런볼")
            .setNull("imageURL")
            .sample();

        // when
        foodService.updateFood(saved.getId(), updateInfo, key, newCategory.getId());

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood.getCategory().getId()).isEqualTo(newCategory.getId());
    }

    @Test
    void 음식_재고를_추가하면_수량이_증가한다() {
        // given
        Quantity initialQuantity = Quantity.register(new BigDecimal("10.0"), Unit.L);
        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", initialQuantity)
                .sample()
        );

        FoodStockUpdateRequest request = new FoodStockUpdateRequest(
            new BigDecimal("2.5"), Unit.L, StockActionType.ADD
        );

        // when
        foodService.updateFoodStock(saved.getId(), request, key);

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood.getQuantity().getAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(12.5));
    }

    @Test
    void 음식_재고를_소비하면_수량이_감소한다() {
        // given
        Quantity initialQuantity = Quantity.register(new BigDecimal("5.00"), Unit.PIECE);
        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", initialQuantity)
                .sample()
        );

        FoodStockUpdateRequest request = new FoodStockUpdateRequest(
            new BigDecimal("2.00"), Unit.PIECE, StockActionType.CONSUME
        );

        // when
        foodService.updateFoodStock(saved.getId(), request, key);

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood.getQuantity().getAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    void 음식_수정_시_이미지_URL이_변경되면_새로운_이미지로_교체된다() {
        // given
        String oldImageUrl = "https://s3.example.com/images/old.jpg";
        String newImageUrl = "https://s3.example.com/images/new.jpg";

        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", oldImageUrl)
                .sample()
        );

        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", newImageUrl)
            .sample();

        // when
        foodService.updateFood(saved.getId(), updateInfo, key, category.getId());

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood.getImageURL()).isEqualTo(newImageUrl);
    }

    @Test
    void 음식_수정_시_이미지_URL이_같으면_이미지가_유지된다() {
        // given
        String sameImageUrl = "https://s3.example.com/images/same.jpg";

        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", sameImageUrl)
                .sample()
        );

        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .set("imageURL", sameImageUrl)
            .sample();

        // when
        foodService.updateFood(saved.getId(), updateInfo, key, category.getId());

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood.getImageURL()).isEqualTo(sameImageUrl);
    }

    @Test
    void 음식_수정_시_이미지_URL이_null이면_기존_이미지가_유지된다() {
        // given
        String existingImageUrl = "https://s3.example.com/images/existing.jpg";

        Food saved = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", existingImageUrl)
                .sample()
        );

        FoodInfo updateInfo = fixtureMonkey.giveMeBuilder(FoodInfo.class)
            .setNull("imageURL")
            .set("name", "이름만 수정")
            .sample();

        // when
        foodService.updateFood(saved.getId(), updateInfo, key, category.getId());

        // then
        Food updatedFood = foodRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedFood)
            .extracting("imageURL", "name")
            .containsExactly(existingImageUrl, "이름만 수정");
    }

    @Test
    void 존재하지_않는_음식을_수정하면_예외가_발생한다() {
        // given
        long nonExistentFoodId = 999L;
        FoodInfo updateInfo = fixtureMonkey.giveMeOne(FoodInfo.class);

        // expected
        assertThatThrownBy(() -> foodService.updateFood(nonExistentFoodId, updateInfo, key, category.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    private Food createFood() {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("imageURL", "https://s3.example.com/images/uuid-test.jpg")
                .sample()
        );
    }

    private void createFoodWithStatus(FoodStatus status) {
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("foodStatus", status)
                .sample()
        );
    }
}
