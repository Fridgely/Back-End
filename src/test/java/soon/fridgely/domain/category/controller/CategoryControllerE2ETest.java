package soon.fridgely.domain.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.dto.request.CategoryModifyRequest;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.E2ETestSupport;
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class CategoryControllerE2ETest extends E2ETestSupport {

    private static final String BASE_URL = "/api/v1/refrigerators";

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

    @Test
    void 카테고리를_추가한다() {
        // given
        setupBasicEnvironment();
        var request = fixtureMonkey.giveMeBuilder(CategoryAddRequest.class)
            .set("name", "국수")
            .sample();

        var httpEntity = createAuthEntity(request, member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories",
            HttpMethod.POST,
            httpEntity,
            responseType
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Category savedCategory = categoryRepository.findByNameAndRefrigeratorIdAndStatus("국수", refrigerator.getId(), EntityStatus.ACTIVE).orElseThrow();
        assertThat(savedCategory)
            .extracting("name", "type")
            .containsExactly("국수", CategoryType.CUSTOM);
    }

    @Test
    void 카테고리_단건을_조회한다() {
        // given
        setupBasicEnvironment();

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<CategoryDetailResponse>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories/" + category.getId(),
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result", "body.data")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS, new CategoryDetailResponse(category.getId(), category.getName(), category.isDefaultType()));
    }

    @Test
    void 카테고리_전체를_조회한다() {
        // given
        setupBasicEnvironment();
        Category anotherCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<List<CategoryResponse>>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories",
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data())
            .hasSize(2) // setupBasicEnvironment의 category + anotherCategory
            .extracting("id")
            .contains(category.getId(), anotherCategory.getId());
    }

    @Test
    void 카테고리를_수정한다() {
        // given
        setupBasicEnvironment();
        var request = fixtureMonkey.giveMeBuilder(CategoryModifyRequest.class)
            .set("newName", "채소")
            .sample();

        var httpEntity = createAuthEntity(request, member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories/" + category.getId(),
            HttpMethod.PATCH,
            httpEntity,
            responseType
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Category updatedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(updatedCategory.getName()).isEqualTo("채소");
    }

    @Test
    void 카테고리를_삭제한다_음식은_기타_카테고리로_이동된다() {
        // given
        setupBasicEnvironment();

        Category fallbackCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member)
                .set("name", "기타")
                .set("type", CategoryType.DEFAULT)
                .sample()
        );

        createFoods(3, category);

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories/" + category.getId(),
            HttpMethod.DELETE,
            httpEntity,
            responseType
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Category deletedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();

        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
    }

    private void setupBasicEnvironment() {
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
            category(fixtureMonkey, refrigerator, member)
                .set("type", CategoryType.CUSTOM)
                .sample()
        );
    }

    private void createFoods(int count, Category targetCategory) {
        List<Food> foods = IntStream.range(0, count)
            .mapToObj(i -> food(fixtureMonkey, refrigerator, member, targetCategory)
                .set("createdAt", LocalDate.now().atStartOfDay())
                .sample())
            .toList();
        foodRepository.saveAll(foods);
    }

}