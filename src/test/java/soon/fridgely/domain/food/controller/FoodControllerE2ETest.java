package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import soon.fridgely.E2ETestSupport;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.security.provider.TokenProvider;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

    @Autowired
    private TokenProvider tokenProvider;

    @MockitoBean
    private ImageManager imageManager;

    @Test
    void 음식을_등록한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("육류", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        given(imageManager.upload(any())).willReturn("http://fake-s3-url.com/image.jpg");

        var foodRequest = new FoodCreateRequest(
            "삼겹살",
            category.getId(),
            new BigDecimal("600"),
            Unit.G,
            LocalDateTime.now().plusDays(5),
            StorageType.REFRIGERATION,
            "구워먹을 예정"
        );

        var jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        var requestPart = new HttpEntity<>(foodRequest, jsonHeaders);

        var imageResource = new ByteArrayResource("fake-image".getBytes()) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("request", requestPart);
        body.add("image", imageResource);

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var httpEntity = new HttpEntity<>(body, headers);

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
            .containsExactly(
                tuple("삼겹살", "구워먹을 예정")
            );
    }

    @Test
    void 음식_단건을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

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
            .containsExactly("testFood", "과일");
    }

    @Test
    void 음식_목록을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food1 = createFood(refrigerator, member, category, LocalDate.now());
        Food food2 = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.saveAll(List.of(food1, food2));

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

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
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category oldCategory = Category.register("과자", refrigerator, member, CategoryType.CUSTOM);
        Category newCategory = Category.register("냉동", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.saveAll(List.of(oldCategory, newCategory));

        Food food = createFood(refrigerator, member, oldCategory, LocalDate.now());
        foodRepository.save(food);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var updateRequest = new FoodUpdateRequest(
            "수정된 이름",
            newCategory.getId(),
            new BigDecimal("2.0"),
            Unit.KG,
            LocalDateTime.now().plusDays(10),
            StorageType.ROOM_TEMPERATURE,
            "설명 수정"
        );

        var jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        var requestPart = new HttpEntity<>(updateRequest, jsonHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("request", requestPart);

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var httpEntity = new HttpEntity<>(body, headers);

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
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, LocalDate.now());
        foodRepository.save(food);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

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

    // Slice 역직렬화 오류 해결을 위한 임시 Slice 구현체
    private record TestPage<T>(List<T> content, boolean last, int size, int number) {
    }

}