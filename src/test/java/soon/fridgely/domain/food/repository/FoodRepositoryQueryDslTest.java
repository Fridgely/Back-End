package soon.fridgely.domain.food.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodRepositoryQueryDslTest extends IntegrationTestSupport {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );
    }

    @Test
    void 유통기한_오름차순_정렬로_조회한다() {
        // given
        LocalDate now = LocalDate.now();
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "음식3")
                .set("expirationDate", now.plusDays(30).atStartOfDay())
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "음식1")
                .set("expirationDate", now.plusDays(5).atStartOfDay())
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "음식2")
                .set("expirationDate", now.plusDays(10).atStartOfDay())
                .sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.EXPIRATION,
            null,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent())
            .extracting("name")
            .containsExactly("음식1", "음식2", "음식3");
    }

    @Test
    void 등록일_내림차순_정렬로_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category).set("name", "음식1").sample(),
            food(fixtureMonkey, refrigerator, member, category).set("name", "음식2").sample(),
            food(fixtureMonkey, refrigerator, member, category).set("name", "음식3").sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.CREATED,
            null,
            PageRequest.ofSize(10)
        );

        // then - 최신순 (createdAt DESC)
        assertThat(result.getContent())
            .extracting("name")
            .containsExactly("음식3", "음식2", "음식1");
    }

    @Test
    void 이름_오름차순_정렬로_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category).set("name", "치킨").sample(),
            food(fixtureMonkey, refrigerator, member, category).set("name", "김치").sample(),
            food(fixtureMonkey, refrigerator, member, category).set("name", "사과").sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.NAME,
            null,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent())
            .extracting("name")
            .containsExactly("김치", "사과", "치킨");
    }

    @Test
    void 저장_위치로_필터링하고_유통기한_오름차순으로_조회한다() {
        // given
        LocalDate now = LocalDate.now();
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉장음식1")
                .set("storageType", StorageType.REFRIGERATION)
                .set("expirationDate", now.plusDays(5).atStartOfDay())
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉동음식")
                .set("storageType", StorageType.FROZEN)
                .set("expirationDate", now.plusDays(1).atStartOfDay())
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉장음식2")
                .set("storageType", StorageType.REFRIGERATION)
                .set("expirationDate", now.plusDays(3).atStartOfDay())
                .sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.EXPIRATION,
            StorageType.REFRIGERATION,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent()).hasSize(2)
            .extracting("name")
            .containsExactly("냉장음식2", "냉장음식1");
    }

    @Test
    void 저장_위치로_필터링하고_이름_오름차순으로_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "치킨")
                .set("storageType", StorageType.FROZEN)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "우유")
                .set("storageType", StorageType.REFRIGERATION)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "만두")
                .set("storageType", StorageType.FROZEN)
                .sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.NAME,
            StorageType.FROZEN,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent()).hasSize(2)
            .extracting("name")
            .containsExactly("만두", "치킨");
    }

    @Test
    void 저장_위치로_필터링하고_등록일_내림차순으로_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "음식1")
                .set("storageType", StorageType.ROOM_TEMPERATURE)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "음식2")
                .set("storageType", StorageType.ROOM_TEMPERATURE)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "냉장음식")
                .set("storageType", StorageType.REFRIGERATION)
                .sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.CREATED,
            StorageType.ROOM_TEMPERATURE,
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent()).hasSize(2)
            .extracting("name")
            .containsExactly("음식2", "음식1");
    }

    @Test
    void storageType이_null이면_전체_음식을_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("storageType", StorageType.REFRIGERATION)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("storageType", StorageType.FROZEN)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("storageType", StorageType.ROOM_TEMPERATURE)
                .sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.CREATED,
            null, // 전체 조회
            PageRequest.ofSize(10)
        );

        // then
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void 카테고리가_Fetch_Join되어_N플러스1_문제가_발생하지_않는다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category).sample(),
            food(fixtureMonkey, refrigerator, member, category).sample()
        ));

        // when
        Slice<Food> result = foodRepository.findAllDynamic(
            refrigerator.getId(),
            Long.MAX_VALUE,
            FoodSortType.CREATED,
            null,
            PageRequest.ofSize(10)
        );

        // then - 카테고리가 Fetch Join되어 있어야 함
        assertThat(result.getContent())
            .allSatisfy(food -> assertThat(food.getCategory().getName()).isNotNull());
    }

}