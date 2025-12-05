package soon.fridgely.domain.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import soon.fridgely.E2ETestSupport;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.auth.provider.TokenProvider;
import soon.fridgely.domain.category.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.dto.request.CategoryModifyRequest;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
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
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    void 카테고리를_추가한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var request = new CategoryAddRequest("국수");
        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(request, headers);

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
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.CREATED, ResultType.SUCCESS);

        Category savedCategory = categoryRepository.findByNameAndRefrigeratorIdAndStatus("국수", refrigerator.getId(), EntityStatus.ACTIVE).orElseThrow();
        assertThat(savedCategory).isNotNull()
            .extracting("name", "type")
            .containsExactly("국수", CategoryType.CUSTOM);
    }

    @Test
    void 카테고리_단건을_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

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
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS, new CategoryDetailResponse(category.getId(), "과일", false));
    }

    @Test
    void 카테고리_전체를_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category1 = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        Category category2 = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.saveAll(List.of(category1, category2));

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

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
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result", "body.data")
            .containsExactly(
                HttpStatus.OK,
                ResultType.SUCCESS,
                List.of(
                    new CategoryResponse(category1.getId(), "과일", false),
                    new CategoryResponse(category2.getId(), "채소", false)
                )
            );
    }

    @Test
    void 카테고리를_수정한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var request = new CategoryModifyRequest("채소");
        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(request, headers);

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
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        Category updatedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(updatedCategory.getName()).isEqualTo("채소");
    }

    @Test
    void 카테고리를_삭제한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category targetCategory = Category.register("과일", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, targetCategory, LocalDate.now()))
            .limit(3)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Void>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/" + refrigerator.getId() + "/categories/" + targetCategory.getId(),
            HttpMethod.DELETE,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        Category deletedCategory = categoryRepository.findById(targetCategory.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();

        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods).hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getId())
                .isEqualTo(fallbackCategory.getId())
            );
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