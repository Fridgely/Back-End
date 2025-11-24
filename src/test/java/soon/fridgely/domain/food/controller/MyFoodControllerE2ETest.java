package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import soon.fridgely.E2ETestSupport;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
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
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MyFoodControllerE2ETest extends E2ETestSupport {

    private static final String BASE_URL = "/api/v1/foods";

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
    void 내_모든_음식을_상태별로_그룹핑하여_조회한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        LocalDate now = LocalDate.now();
        Food blackFood = createFood(refrigerator, member, category, now.minusDays(1).atStartOfDay(), now);
        Food redFood = createFood(refrigerator, member, category, now.plusDays(3).atStartOfDay(), now);
        Food greenFood = createFood(refrigerator, member, category, now.plusDays(30).atStartOfDay(), now);
        foodRepository.saveAll(List.of(blackFood, redFood, greenFood));

        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();

        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        var httpEntity = new HttpEntity<>(headers);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<FoodStatusResponse>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL + "/status",
            HttpMethod.GET,
            httpEntity,
            responseType
        );

        // then
        assertThat(response).isNotNull()
            .extracting("statusCode", "body.result")
            .containsExactly(HttpStatus.OK, ResultType.SUCCESS);

        var result = response.getBody().data();
        assertGroup(result.black(), blackFood);
        assertGroup(result.red(), redFood);
        assertGroup(result.green(), greenFood);
        assertThat(result.yellow()).isEmpty();
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, LocalDateTime expirationDate, LocalDate now) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            expirationDate,
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            now
        );
    }

    private void assertGroup(List<FoodResponse> actualList, Food expectedFood) {
        assertThat(actualList)
            .as("그룹핑 결과 확인")
            .hasSize(1)
            .extracting(FoodResponse::id)
            .containsExactly(expectedFood.getId());
    }

}