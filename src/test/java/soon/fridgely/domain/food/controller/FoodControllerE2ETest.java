package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StockActionType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.E2ETestSupport;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class FoodControllerE2ETest extends E2ETestSupport {

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

    @MockitoBean
    private ImageManager imageManager;

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @Test
    void 음식을_등록한다() {
        // given
        setupBasicEnvironment();
        given(imageManager.upload(any())).willReturn("http://fake-s3-url.com/image.jpg");

        var foodRequest = fixtureMonkey.giveMeBuilder(FoodCreateRequest.class)
            .set("name", "삼겹살")
            .set("categoryId", category.getId())
            .set("description", "구워먹을 예정")
            .sample();

        var httpEntity = createMultipartRequest(foodRequest);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods",
            HttpMethod.POST,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.CREATED, ResultType.SUCCESS);

        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1)
            .extracting("name", "description")
            .containsExactly(tuple("삼겹살", "구워먹을 예정"));
    }

    @Test
    void 음식_단건을_조회한다() {
        // given
        setupBasicEnvironment();
        Food food = createFood();
        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<FoodDetailResponse>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods/" + food.getId(),
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        assertThat(response.getBody().data())
            .extracting("name", "categoryName")
            .containsExactly(food.getName(), category.getName());
    }

    @Test
    void 음식_목록을_조회한다() {
        // given
        setupBasicEnvironment();
        createFood();
        createFood();

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<TestPage<FoodResponse>>>() {
        };

        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods",
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        assertThat(response.getBody().data().content()).hasSize(2);
        assertThat(response.getBody().data().last()).isTrue();
    }

    @Test
    void 음식을_수정한다() {
        // given
        setupBasicEnvironment();
        Food food = createFood();

        Category newCategory = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );

        var updateRequest = fixtureMonkey.giveMeBuilder(FoodUpdateRequest.class)
            .set("name", "수정된 이름")
            .set("description", "설명 수정")
            .set("categoryId", newCategory.getId())
            .set("amount", new BigDecimal("2.00"))
            .sample();

        var httpEntity = createMultipartRequest(updateRequest);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods/" + food.getId(),
            HttpMethod.PATCH,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood)
            .extracting("name", "description", "category.id", "quantity.amount")
            .containsExactly("수정된 이름", "설명 수정", newCategory.getId(), new BigDecimal("2.00"));
    }

    @Test
    void 음식을_삭제한다() {
        // given
        setupBasicEnvironment();
        Food food = createFood();

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods/" + food.getId(),
            HttpMethod.DELETE,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        Food deletedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(deletedFood.getStatus()).isEqualTo(EntityStatus.DELETED);
    }

    @Test
    void 식재료_재고를_조정한다() {
        // given
        setupBasicEnvironment();

        Quantity initialQuantity = new Quantity(new BigDecimal("5.0"), Unit.PIECE);
        Food food = foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("quantity", initialQuantity)
                .sample()
        );

        var stockRequest = fixtureMonkey.giveMeBuilder(FoodStockUpdateRequest.class)
            .set("amount", new BigDecimal("2.0"))
            .set("unit", Unit.PIECE)
            .set("action", StockActionType.CONSUME)
            .sample();
        var httpEntity = createAuthEntity(stockRequest, member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods/" + food.getId() + "/stock",
            HttpMethod.PATCH,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        Food updatedFood = foodRepository.findById(food.getId()).orElseThrow();
        assertThat(updatedFood.getQuantity().amount())
            .isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void 식재료_목록을_유통기한_임박순으로_조회한다() {
        // given
        setupBasicEnvironment();
        LocalDate now = LocalDate.now();

        createFoodWithExpiration("음식1", now.plusDays(30));
        createFoodWithExpiration("음식2", now.plusDays(5));
        createFoodWithExpiration("음식3", now.plusDays(15));

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<TestPage<FoodResponse>>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods?size=10&sortBy=EXPIRATION",
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        var content = response.getBody().data().content();
        assertThat(content)
            .extracting("name")
            .containsExactly("음식2", "음식3", "음식1");
    }

    @Test
    void 식재료_목록을_이름순으로_조회한다() {
        // given
        setupBasicEnvironment();

        createFoodWithName("토마토");
        createFoodWithName("감자");
        createFoodWithName("당근");

        var httpEntity = createAuthEntity(member);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<TestPage<FoodResponse>>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/foods?size=10&sortBy=NAME",
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        var content = response.getBody().data().content();
        assertThat(content)
            .extracting("name")
            .containsExactly("감자", "당근", "토마토");
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

    private Food createFood() {
        return foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category).sample()
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

    private HttpEntity<MultiValueMap<String, Object>> createMultipartRequest(Object requestDto) {
        var jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        var requestPart = new HttpEntity<>(requestDto, jsonHeaders);
        var imageResource = new ByteArrayResource("fake-image".getBytes()) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("request", requestPart);
        body.add("image", imageResource);

        var headers = createAuthHeaders(member);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    // Slice 역직렬화 오류 해결을 위한 임시 Slice 구현체
    private record TestPage<T>(List<T> content, boolean last, int size, int number) {
    }

}