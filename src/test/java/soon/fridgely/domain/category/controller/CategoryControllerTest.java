package soon.fridgely.domain.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.category.controller.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.controller.dto.request.CategoryModifyRequest;
import soon.fridgely.domain.category.service.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.service.dto.response.CategoryResponse;
import soon.fridgely.global.support.annotation.TestLoginMember;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest extends ControllerTestSupport {

    public static final String BASE_URL = "/api/v1/refrigerators";

    @TestLoginMember
    @Test
    void 카테고리를_추가한다() throws Exception {
        // given
        var request = new CategoryAddRequest("newCategory");
        long refrigeratorId = 1L;

        // expected
        mockMvc.perform(
                post(BASE_URL + "/" + refrigeratorId + "/categories")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 카테고리를_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long memberId = 1L;
        long categoryId = 1L;

        var response = new CategoryDetailResponse(1L, "category", true);

        given(categoryService.findCategory(categoryId, refrigeratorId, memberId))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/" + refrigeratorId + "/categories/" + categoryId)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("category"))
            .andExpect(jsonPath("$.data.custom").value(true));
    }

    @TestLoginMember
    @Test
    void 카테고리_목록을_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long memberId = 1L;

        var response = List.of(
            new CategoryResponse(1L, "category1", true),
            new CategoryResponse(2L, "category2", false)
        );

        given(categoryService.findAllCategory(refrigeratorId, memberId))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/" + refrigeratorId + "/categories")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @TestLoginMember
    @Test
    void 카테고리를_수정한다() throws Exception {
        // given
        var request = new CategoryModifyRequest("newCategoryName");
        long refrigeratorId = 1L;
        long categoryId = 1L;

        // expected
        mockMvc.perform(
                patch(BASE_URL + "/" + refrigeratorId + "/categories/" + categoryId)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 카테고리를_삭제한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long categoryId = 1L;

        // expected
        mockMvc.perform(
                delete(BASE_URL + "/" + refrigeratorId + "/categories/" + categoryId)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

}