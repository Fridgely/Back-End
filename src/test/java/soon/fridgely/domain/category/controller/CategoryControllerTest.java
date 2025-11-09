package soon.fridgely.domain.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.category.controller.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.controller.dto.request.CategoryModifyRequest;
import soon.fridgely.global.support.annotation.TestLoginMember;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

}