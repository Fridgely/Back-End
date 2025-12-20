package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyFoodControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/foods";

    @TestLoginMember
    @Test
    void 내_모든_음식을_상태별로_그룹핑하여_조회한다() throws Exception {
        // given
        var redFood = fixtureMonkey.giveMeBuilder(FoodResponse.class)
            .set("id", 1L)
            .set("condition.foodStatus", FoodStatus.RED)
            .sample();
        var greenFood = fixtureMonkey.giveMeBuilder(FoodResponse.class)
            .set("id", 2L)
            .set("condition.foodStatus", FoodStatus.GREEN)
            .sample();

        var response = fixtureMonkey.giveMeBuilder(FoodStatusResponse.class)
            .set("black", List.of())
            .set("red", List.of(redFood))
            .set("yellow", List.of())
            .set("green", List.of(greenFood))
            .sample();

        given(foodService.findAllMyFoodsGroupedByStatus(anyLong()))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/status")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.red").isArray())
            .andExpect(jsonPath("$.data.red[0].id").value(redFood.id()))
            .andExpect(jsonPath("$.data.red[0].condition.foodStatus").value("RED"))
            .andExpect(jsonPath("$.data.green").isArray())
            .andExpect(jsonPath("$.data.green[0].id").value(greenFood.id()))
            .andExpect(jsonPath("$.data.green[0].condition.foodStatus").value("GREEN"))
            .andExpect(jsonPath("$.data.black").isEmpty())
            .andExpect(jsonPath("$.data.yellow").isEmpty());
    }

}