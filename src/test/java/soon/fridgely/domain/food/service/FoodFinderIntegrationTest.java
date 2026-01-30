package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalDate;
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

    @Test
    void 유통기한_임박순으로_음식을_조회한다() {
        // given
        LocalDate now = LocalDate.now();
        createFoodWithExpiration("음식1", now.plusDays(30));
        createFoodWithExpiration("음식2", now.plusDays(5));
        createFoodWithExpiration("음식3", now.plusDays(15));

        // when
        Slice<Food> result = foodFinder.findAll(
            refrigerator.getId(),
            Long.MAX_VALUE,
            PageRequest.of(0, 10),
            FoodSortType.EXPIRATION,
            null
        );

        // then
        assertThat(result.getContent())
            .hasSize(3)
            .extracting("name")
            .containsExactly("음식2", "음식3", "음식1");
    }

    @Test
    void 등록순으로_음식을_조회한다() {
        // given
        createFoodWithName("음식1");
        createFoodWithName("음식2");
        createFoodWithName("음식3");

        // when
        Slice<Food> result = foodFinder.findAll(
            refrigerator.getId(),
            Long.MAX_VALUE,
            PageRequest.of(0, 10),
            FoodSortType.CREATED,
            null
        );

        // then
        assertThat(result.getContent())
            .hasSize(3)
            .extracting("name")
            .containsExactly("음식3", "음식2", "음식1");
    }

    @Test
    void 이름순으로_음식을_조회한다() {
        // given
        createFoodWithName("토마토");
        createFoodWithName("감자");
        createFoodWithName("당근");

        // when
        Slice<Food> result = foodFinder.findAll(
            refrigerator.getId(),
            Long.MAX_VALUE,
            PageRequest.of(0, 10),
            FoodSortType.NAME,
            null
        );

        // then
        assertThat(result.getContent())
            .hasSize(3)
            .extracting("name")
            .containsExactly("감자", "당근", "토마토");
    }

    @Test
    void 저장_위치로_필터링하여_음식을_조회한다() {
        // given
        createFoodWithStorageType("냉장음식", StorageType.REFRIGERATION);
        createFoodWithStorageType("냉동음식", StorageType.FROZEN);

        // when
        Slice<Food> result = foodFinder.findAll(
            refrigerator.getId(),
            Long.MAX_VALUE,
            PageRequest.of(0, 10),
            FoodSortType.EXPIRATION,
            StorageType.REFRIGERATION
        );

        // then
        assertThat(result.getContent()).hasSize(1)
            .extracting("name")
            .containsExactly("냉장음식");
    }

    private void createFood(FoodStatus status) {
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("foodStatus", status)
                .sample()
        );
    }

    private Food createFoodWithName(String name) {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", name)
                .sample()
        );
    }

    private Food createFoodWithExpiration(String name, LocalDate expirationDate) {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", name)
                .set("expirationDate", expirationDate.atStartOfDay())
                .sample()
        );
    }

    private Food createFoodWithStorageType(String name, StorageType storageType) {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", name)
                .set("storageType", storageType)
                .sample()
        );
    }

}