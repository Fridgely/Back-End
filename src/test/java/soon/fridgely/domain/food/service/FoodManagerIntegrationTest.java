package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodCondition;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class FoodManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FoodManager foodManager;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 카테고리의_모든_음식을_기본_카테고리로_이동한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, targetCategory, LocalDate.now()))
            .limit(3)
            .toList();
        foodRepository.saveAll(foods);

        // when
        foodManager.moveAllFoodsToFallback(refrigerator.getId(), targetCategory.getId());

        // then
        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(food -> assertThat(food.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
    }

    @Test
    void 음식을_등록한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        FoodCondition condition = new FoodCondition(LocalDateTime.now().plusDays(5L), StorageType.ROOM_TEMPERATURE);
        FoodInfo foodInfo = new FoodInfo(
            "홈런볼",
            new Quantity(BigDecimal.ONE, Unit.KG),
            condition,
            "초코맛",
            "http://example.com/image.jpg"
        );

        // when
        foodManager.createFood(foodInfo, new MemberRefrigeratorKey(member.getId(), refrigerator.getId()), category.getId());

        // then
        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1)
            .extracting("name", "description")
            .containsExactly(tuple("홈런볼", "초코맛"));
    }

    @Test
    void 음식을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        // when
        Food found = foodManager.find(food.getId(), refrigerator.getId());

        // then
        assertThat(found).isNotNull()
            .extracting("name", "description")
            .containsExactly("testFood", "testDescription");
    }

    @Test
    void 음식_정보_수정_시_카테고리가_변경되지_않으면_기존_카테고리를_유지한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        FoodInfo updateInfo = new FoodInfo(
            "수정된 홈런볼",
            new Quantity(new BigDecimal("2.0"), Unit.KG),
            new FoodCondition(
                LocalDateTime.now().plusDays(30),
                StorageType.ROOM_TEMPERATURE
            ),
            "바나나맛",
            "http://example.com/new-image.jpg"
        );

        // when
        foodManager.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            category.getId() // 기존 카테고리 ID 전달
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
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category oldCategory = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        Category newCategory = Category.register("냉동식품", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.saveAll(List.of(oldCategory, newCategory));

        Food food = createFood(refrigerator, member, oldCategory, LocalDate.now());
        foodRepository.save(food);

        FoodInfo updateInfo = new FoodInfo(
            food.getName(),
            food.getQuantity(),
            new FoodCondition(food.getExpirationDate(), food.getStorageType()),
            food.getDescription(),
            food.getImageURL()
        );

        // when
        foodManager.update(
            food.getId(),
            updateInfo,
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId()),
            newCategory.getId() // 새로운 카테고리 ID 전달
        );

        // then
        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood.getCategory().getId()).isEqualTo(newCategory.getId());
    }

    @Test
    void 음식을_삭제하면_조회되지_않아야_한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        // when
        foodManager.delete(food.getId(), refrigerator.getId());

        // then
        assertThatThrownBy(() -> foodManager.find(food.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);

        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    @Test
    void 음식을_중복_삭제해도_예외가_발생하지_않는다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        // when
        foodManager.delete(food.getId(), refrigerator.getId());
        foodManager.delete(food.getId(), refrigerator.getId());

        // then
        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.isDeleted()).isTrue();
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, LocalDate now) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            now
        );
    }

}