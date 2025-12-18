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
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Transactional // 해당 메서드는 @Modifying 쿼리를 테스트하므로 트랜잭션이 필요
    @Test
    void 대상_카테고리의_모든_음식을_폴백_카테고리로_이동한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("삭제될 카테고리", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, targetCategory, LocalDate.now()))
            .limit(3)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

        // when
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
        em.clear();

        // then
        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getName()).isEqualTo("기타"));
    }

    @Test
    void 특정_냉장고의_ACTIVE_음식만_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator1 = Refrigerator.register(member.getNickname());
        Refrigerator refrigerator2 = Refrigerator.register(member.getNickname());
        refrigeratorRepository.saveAll(List.of(refrigerator1, refrigerator2));

        Category category1 = Category.register("채소", refrigerator1, member, CategoryType.DEFAULT);
        Category category2 = Category.register("육류", refrigerator2, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(category1, category2));

        List<Food> activeFoods = Stream.generate(() -> createFood(refrigerator1, member, category1, LocalDate.now()))
            .limit(3)
            .collect(Collectors.toList());
        foodRepository.saveAll(activeFoods);

        Food deletedFood = createFood(refrigerator1, member, category1, LocalDate.now());
        deletedFood.delete();
        foodRepository.save(deletedFood);

        Food otherFridgeFood = createFood(refrigerator2, member, category2, LocalDate.now());
        foodRepository.save(otherFridgeFood);

        Pageable pageable = PageRequest.ofSize(10);

        // when
        Slice<Food> result = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator1.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            pageable
        );

        // then
        assertThat(result.getContent()).hasSize(3)
            .allSatisfy(food -> {
                assertThat(food.getRefrigerator().getId()).isEqualTo(refrigerator1.getId());
                assertThat(food.getStatus()).isEqualTo(EntityStatus.ACTIVE);
            });
    }

    @Test
    void 음식_목록을_ID_내림차순으로_정렬하여_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, category, LocalDate.now()))
            .limit(5)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

        Pageable pageable = PageRequest.ofSize(10);

        // when
        Slice<Food> result = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            pageable
        );

        // then
        List<Long> ids = result.getContent().stream()
            .map(Food::getId)
            .collect(Collectors.toList());

        List<Long> sortedIdsDesc = ids.stream()
            .sorted((a, b) -> Long.compare(b, a))
            .toList();

        assertThat(ids).isEqualTo(sortedIdsDesc);
    }

    @Test
    void 커서_기반_페이징으로_첫_페이지를_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, category, LocalDate.now()))
            .limit(5)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

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
    }

    @Test
    void 커서_기반_페이징으로_다음_페이지를_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, category, LocalDate.now()))
            .limit(5)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

        Pageable pageable = PageRequest.ofSize(3);

        Slice<Food> firstSlice = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            Long.MAX_VALUE,
            EntityStatus.ACTIVE,
            pageable
        );

        Long lastIdOfFirstSlice = firstSlice.getContent()
            .get(firstSlice.getNumberOfElements() - 1)
            .getId();

        // when
        Slice<Food> secondSlice = foodRepository.findAllByRefrigeratorWithCategory(
            refrigerator.getId(),
            lastIdOfFirstSlice,
            EntityStatus.ACTIVE,
            pageable
        );

        // then
        assertThat(secondSlice.getNumberOfElements()).isEqualTo(2);
        assertThat(secondSlice.hasNext()).isFalse();

        assertThat(secondSlice.getContent())
            .allSatisfy(food -> assertThat(food.getId()).isLessThan(lastIdOfFirstSlice));
    }

    @Test
    void 내가_참여한_냉장고의_음식들만_유통기한_오름차순으로_조회된다() {
        // given
        Member me = createMember("me", "testId1");
        Member other = createMember("other", "testId2");
        memberRepository.saveAll(List.of(me, other));

        Refrigerator myFridge = Refrigerator.register(me.getNickname());
        Refrigerator otherFridge = Refrigerator.register(other.getNickname());
        refrigeratorRepository.saveAll(List.of(myFridge, otherFridge));

        MemberRefrigerator meLink = MemberRefrigerator.link(me, myFridge, RefrigeratorRole.OWNER);
        MemberRefrigerator otherLink = MemberRefrigerator.link(other, otherFridge, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.saveAll(List.of(meLink, otherLink));

        Category myCategory = Category.register("채소", myFridge, me, CategoryType.DEFAULT);
        Category otherCategory = Category.register("채소", otherFridge, other, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(myCategory, otherCategory));

        LocalDate now = LocalDate.now();
        Food myFood1 = createFood(myFridge, me, myCategory, now.plusDays(5));
        Food myFood2 = createFood(myFridge, me, myCategory, now.plusDays(1));
        Food otherFood = createFood(otherFridge, other, otherCategory, now.plusDays(1));
        foodRepository.saveAll(List.of(myFood1, myFood2, otherFood));

        // when
        List<Food> result = foodRepository.findAllMyFoods(me.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(result).hasSize(2)
            .extracting("id")
            .containsExactly(myFood2.getId(), myFood1.getId());

        assertThat(result)
            .allSatisfy(food -> assertThat(food.getCategory().getName())
                .isEqualTo(myCategory.getName()));
    }

    @Test
    void 특정_회원의_음식_중_지정된_날짜_범위에_만료되는_음식을_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        LocalDate targetDate = LocalDate.of(2025, 12, 25);
        Food targetFood1 = createFood(refrigerator, member, category, targetDate.atTime(10, 0), LocalDate.now());
        Food targetFood2 = createFood(refrigerator, member, category, targetDate.atTime(23, 59), LocalDate.now());
        Food yesterdayFood = createFood(refrigerator, member, category, targetDate.minusDays(1).atStartOfDay(), LocalDate.now());
        Food tomorrowFood = createFood(refrigerator, member, category, targetDate.plusDays(1).atStartOfDay(), LocalDate.now());
        foodRepository.saveAll(List.of(targetFood1, targetFood2, yesterdayFood, tomorrowFood));

        // when
        List<Food> results = foodRepository.findMyFoodsExpiringBetween(member.getId(), targetDate.atStartOfDay(), targetDate.atTime(23, 59, 59), EntityStatus.ACTIVE);

        // then
        assertThat(results).hasSize(2)
            .extracting("id")
            .containsExactly(targetFood1.getId(), targetFood2.getId());
    }

    @Test
    void 특정_회원의_음식_중_재고가_0인_음식을_조회한다() {
        // given
        Member member = createMember("testNickname", "testId");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        Food targetFood = createFood(refrigerator, member, category, new Quantity(BigDecimal.ZERO, Unit.PIECE));
        Food food1 = createFood(refrigerator, member, category, new Quantity(BigDecimal.ONE, Unit.PIECE));
        Food food2 = createFood(refrigerator, member, category, new Quantity(BigDecimal.ZERO, Unit.PIECE));
        food2.delete();
        foodRepository.saveAll(List.of(targetFood, food1, food2));

        // when
        List<Food> results = foodRepository.findAllOutOfStock(member.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(results).hasSize(1)
            .extracting("id")
            .containsExactly(targetFood.getId());

        assertThat(results.get(0).getQuantity().amount())
            .isEqualTo(new BigDecimal("0.00"));
    }

    private Member createMember(String testNickname, String testId) {
        return Member.builder()
            .loginId(testId)
            .password("testPassword")
            .nickname(testNickname)
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(
        Refrigerator refrigerator,
        Member member,
        Category category,
        LocalDate expirationDate
    ) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(BigDecimal.ONE, Unit.KG),
            expirationDate.atStartOfDay(),
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            LocalDate.now()
        );
    }

    private Food createFood(
        Refrigerator refrigerator,
        Member member,
        Category category,
        LocalDateTime expirationDateTime,
        LocalDate now
    ) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(BigDecimal.ONE, Unit.KG),
            expirationDateTime,
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            now
        );
    }

    private Food createFood(
        Refrigerator refrigerator,
        Member member,
        Category category,
        Quantity quantity
    ) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            quantity,
            LocalDateTime.now().plusDays(5),
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            LocalDate.now()
        );
    }

}