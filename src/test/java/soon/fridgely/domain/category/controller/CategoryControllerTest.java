package soon.fridgely.domain.category.controller;

import org.junit.jupiter.api.Test;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.ControllerTestSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/refrigerators";

    @TestLoginMember
    @Test
    void 카테고리를_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long categoryId = 1L;

        var response = fixtureMonkey.giveMeBuilder(CategoryDetailResponse.class)
            .set("id", categoryId)
            .set("name", "category")
            .set("isDefaultType", true)
            .sample();

        given(categoryService.findCategory(eq(categoryId), any(MemberRefrigeratorKey.class)))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/" + refrigeratorId + "/categories/" + categoryId)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(categoryId))
            .andExpect(jsonPath("$.data.name").value("category"))
            .andExpect(jsonPath("$.data.isDefaultType").value(true));
    }

    @TestLoginMember
    @Test
    void 카테고리_목록을_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;

        var response = List.of(
            fixtureMonkey.giveMeBuilder(CategoryResponse.class)
                .set("id", 1L)
                .sample(),
            fixtureMonkey.giveMeBuilder(CategoryResponse.class)
                .set("id", 2L)
                .sample()
        );

        given(categoryService.findAllCategory(any(MemberRefrigeratorKey.class)))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/" + refrigeratorId + "/categories")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1L))
            .andExpect(jsonPath("$.data[1].id").value(2L));
    }

}