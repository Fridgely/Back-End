package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.StorageType;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

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

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @Test
    void 내_모든_음식을_상태별로_그룹핑하여_조회한다() {
        // given
        setupBasicEnvironment();
        LocalDate now = LocalDate.now();

        Food blackFood = createFoodWithExpiration(now.minusDays(1).atStartOfDay(), now);
        Food redFood = createFoodWithExpiration(now.plusDays(3).atStartOfDay(), now);
        Food greenFood = createFoodWithExpiration(now.plusDays(30).atStartOfDay(), now);

        var httpEntity = createAuthEntity(member);

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
            category(fixtureMonkey, refrigerator, member).sample()
        );
    }

    private Food createFoodWithExpiration(LocalDateTime expirationDate, LocalDate now) {
        Food validSample = fixtureMonkey.giveMeOne(Food.class);

        // register 메서드 내부에서 status를 계산하기 때문에 팩토리 메서드로 생성
        Food food = Food.register(
            refrigerator,
            member,
            validSample.getName(),
            category,
            validSample.getQuantity(),
            expirationDate,
            StorageType.REFRIGERATION,
            validSample.getDescription(),
            validSample.getImageURL(),
            now
        );
        return foodRepository.save(food);
    }

    private void assertGroup(List<FoodResponse> actualList, Food expectedFood) {
        assertThat(actualList)
            .as("그룹핑 결과 확인")
            .hasSize(1)
            .extracting(FoodResponse::id)
            .containsExactly(expectedFood.getId());
    }

}