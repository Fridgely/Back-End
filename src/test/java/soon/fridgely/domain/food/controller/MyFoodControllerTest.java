package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.domain.food.dto.response.FoodConditionResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.dto.response.QuantityResponse;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.ControllerTestSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        var redFood = createFoodResponse(1L, FoodStatus.RED);
        var greenFood = createFoodResponse(2L, FoodStatus.GREEN);

        var response = new FoodStatusResponse(
            List.of(), // BLACK
            List.of(redFood), // RED
            List.of(), // YELLOW
            List.of(greenFood) // GREEN
        );

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

    private FoodResponse createFoodResponse(long id, FoodStatus status) {
        return new FoodResponse(
            id,
            "FoodName",
            "CategoryName",
            "imageURL",
            new QuantityResponse(BigDecimal.ONE, Unit.PIECE),
            new FoodConditionResponse(LocalDateTime.now(), StorageType.FROZEN, status, 3L)
        );
    }

}