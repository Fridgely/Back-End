package soon.fridgely.domain.food.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @Transactional // 해당 메서드는 @Modifying 쿼리를 테스트하므로 트랜잭션이 필요
    @Test
    void 대상_카테고리의_모든_음식을_폴백_카테고리로_이동한다() {
        // given
        setupBasicEnvironment();

        Category targetCategory = categoryRepository.save(
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

        createFoods(3, targetCategory);

        // when
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
        em.clear();

        // then
        assertThat(foodRepository.findAll())
            .hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getName()).isEqualTo("기타"));
    }

    @Test
    void 특정_냉장고의_ACTIVE_음식만_조회한다() {
        // given
        setupBasicEnvironment();
        createFoods(3, category);

        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("status", EntityStatus.DELETED)
                .sample()
        );

        Refrigerator otherFridge = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        Category otherCategory = categoryRepository.save(category(fixtureMonkey, otherFridge, member).sample());
        foodRepository.save(
            food(fixtureMonkey, otherFridge, member, otherCategory).sample()
        );

        // when
        Slice<Food> result = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent()).hasSize(3)
            .allSatisfy(food -> {
                assertThat(food.getRefrigerator().getId()).isEqualTo(refrigerator.getId());
                assertThat(food.getStatus()).isEqualTo(EntityStatus.ACTIVE);
            });
    }

    @Test
    void 음식_목록을_ID_내림차순으로_정렬하여_조회한다() {
        // given
        setupBasicEnvironment();
        createFoods(5, category);

        // when
        Slice<Food> result = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent())
            .extracting("id", Long.class)
            .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void 커서_기반_페이징으로_첫_페이지를_조회한다() {
        // given
        setupBasicEnvironment();
        List<Food> foods = createFoods(5, category);

        Pageable pageable = PageRequest.ofSize(3);

        // when
        Slice<Food> firstSlice = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            pageable
        );

        // then
        assertThat(firstSlice.getNumberOfElements()).isEqualTo(3);
        assertThat(firstSlice.hasNext()).isTrue();
        assertThat(firstSlice.getContent())
            .extracting("id", Long.class)
            .containsExactly(
                foods.get(4).getId(),
                foods.get(3).getId(),
                foods.get(2).getId()
            );
    }

    @Test
    void 커서_기반_페이징으로_다음_페이지를_조회한다() {
        // given
        setupBasicEnvironment();
        List<Food> foods = createFoods(5, category);
        Pageable pageable = PageRequest.ofSize(3);
        long cursorId = foods.get(2).getId();

        // when
        Slice<Food> secondSlice = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            cursorId,
            EntityStatus.ACTIVE,
            pageable
        );

        // then
        assertThat(secondSlice.getNumberOfElements()).isEqualTo(2);
        assertThat(secondSlice.hasNext()).isFalse();

        assertThat(secondSlice.getContent())
            .extracting("id", Long.class)
            .containsExactly(
                foods.get(1).getId(),
                foods.get(0).getId()
            );
    }

    @Test
    void 내가_참여한_냉장고의_음식들만_유통기한_오름차순으로_조회된다() {
        // given
        Member me = memberRepository.save(
            member(fixtureMonkey)
                .set("name", "me")
                .sample()
        );
        Member other = memberRepository.save(
            member(fixtureMonkey)
                .set("name", "other")
                .sample()
        );

        Refrigerator myFridge = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        Refrigerator otherFridge = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        memberRefrigeratorRepository.saveAll(List.of(
            memberRefrigerator(fixtureMonkey, myFridge, me).sample(),
            memberRefrigerator(fixtureMonkey, otherFridge, other).sample()
        ));

        Category myCategory = categoryRepository.save(
            category(fixtureMonkey, myFridge, me).sample()
        );
        Category otherCategory = categoryRepository.save(
            category(fixtureMonkey, otherFridge, other).sample()
        );

        LocalDate now = LocalDate.now();
        List<Food> foods = foodRepository.saveAll(List.of(
            food(fixtureMonkey, myFridge, me, myCategory)
                .set("expirationDate", now.plusDays(5).atStartOfDay())
                .sample(),
            food(fixtureMonkey, myFridge, me, myCategory)
                .set("expirationDate", now.plusDays(1).atStartOfDay())
                .sample(),
            food(fixtureMonkey, otherFridge, other, otherCategory).sample()
        ));

        // when
        List<Food> result = foodRepository.findAllMyFoods(me.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(result).hasSize(2)
            .extracting("id", Long.class)
            .containsExactly(foods.get(1).getId(), foods.get(0).getId());

        assertThat(result)
            .allSatisfy(food -> assertThat(food.getCategory().getName())
                .isEqualTo(myCategory.getName()));
    }

    @Test
    void 특정_회원의_음식_중_지정된_날짜_범위에_만료되는_음식을_조회한다() {
        // given
        setupBasicEnvironment();
        memberRefrigeratorRepository.save(memberRefrigerator(fixtureMonkey, refrigerator, member).sample());

        LocalDate targetDate = LocalDate.of(2025, 12, 25);
        Food inRange1 = createFoodWithExpirationDate(targetDate.atTime(10, 0));
        Food inRange2 = createFoodWithExpirationDate(targetDate.atTime(23, 59));
        createFoodWithExpirationDate(targetDate.minusDays(1).atStartOfDay());
        createFoodWithExpirationDate(targetDate.plusDays(1).atStartOfDay());

        // when
        List<Food> results = foodRepository.findMyFoodsExpiringBetween(
            member.getId(),
            targetDate.atStartOfDay(),
            targetDate.atTime(23, 59, 59),
            EntityStatus.ACTIVE
        );

        // then
        assertThat(results).hasSize(2)
            .extracting("id", Long.class)
            .containsExactlyInAnyOrder(inRange1.getId(), inRange2.getId());
    }

    @Test
    void 특정_회원의_음식_중_재고가_0인_음식을_조회한다() {
        // given
        setupBasicEnvironment();
        memberRefrigeratorRepository.save(memberRefrigerator(fixtureMonkey, refrigerator, member).sample());

        Food outOfStock = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", new Quantity(BigDecimal.ZERO, Unit.PIECE))
                .sample()
        );
        // 재고 있음
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", new Quantity(BigDecimal.ONE, Unit.PIECE))
                .sample()
        );
        // 재고는 0이지만 삭제된 음식
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", new Quantity(BigDecimal.ZERO, Unit.PIECE))
                .set("status", EntityStatus.DELETED)
                .sample()
        );

        // when
        List<Food> results = foodRepository.findAllOutOfStock(member.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(results).hasSize(1)
            .extracting("id", Long.class)
            .containsExactly(outOfStock.getId());

        assertThat(results.get(0).getQuantity().amount())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void setupBasicEnvironment() {
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

    private List<Food> createFoods(int count, Category targetCategory) {
        List<Food> foods = IntStream.range(0, count)
            .mapToObj(i -> food(fixtureMonkey, refrigerator, member, targetCategory).sample())
            .toList();
        return foodRepository.saveAll(foods);
    }

    private Food createFoodWithExpirationDate(LocalDateTime expirationDate) {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", expirationDate)
                .sample()
        );
    }

}