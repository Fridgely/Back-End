package soon.fridgely.domain.refrigerator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.InvitationCodeJoinRequest;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.security.annotation.TestLoginMember;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefrigeratorControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/refrigerators";

    @TestLoginMember
    @Test
    void 내_냉장고_목록을_조회한다() throws Exception {
        // given
        var responses = List.of(
            new RefrigeratorResponse(1L, "Fridge1", RefrigeratorRole.OWNER, true),
            new RefrigeratorResponse(2L, "Fridge2", RefrigeratorRole.MEMBER, false)
        );

        given(refrigeratorService.findAllMyRefrigerators(anyLong()))
            .willReturn(responses);

        // expected
        mockMvc.perform(
                get(BASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].name").value("Fridge1"))
            .andExpect(jsonPath("$.data[1].role").value("MEMBER"));
    }

    @TestLoginMember
    @Test
    void 냉장고의_상세_정보를_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;

        var response = new RefrigeratorResponse(refrigeratorId, "MyFridge", RefrigeratorRole.OWNER, true);
        given(refrigeratorService.findRefrigerator(any(MemberRefrigeratorKey.class)))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/{refrigeratorId}", refrigeratorId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(refrigeratorId))
            .andExpect(jsonPath("$.data.name").value("MyFridge"));
    }

    @TestLoginMember
    @Test
    void 냉장고_이름을_수정한다() throws Exception {
        // given
        var request = new RefrigeratorUpdateRequest("새로운 이름");

        // expected
        mockMvc.perform(
                patch(BASE_URL + "/{refrigeratorId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 초대_코드를_생성한다() throws Exception {
        // given
        String generatedCode = "ABC12345";

        var response = new InvitationCodeResponse(generatedCode, LocalDateTime.now().plusDays(1));
        given(refrigeratorService.generateInvitationCode(any(MemberRefrigeratorKey.class)))
            .willReturn(response);

        // expected
        mockMvc.perform(
                post(BASE_URL + "/{refrigeratorId}/invitation-codes", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.code").value(generatedCode));
    }

    @TestLoginMember
    @Test
    void 초대_코드로_냉장고에_참여한다() throws Exception {
        // given
        var request = new InvitationCodeJoinRequest("ABC12345");

        // expected
        mockMvc.perform(
                post(BASE_URL + "/invitation-codes/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 유효하지_않은_초대코드는_예외가_발생한다() throws Exception {
        // given
        var request = new InvitationCodeJoinRequest("SHORT");

        // expected
        mockMvc.perform(
                post(BASE_URL + "/invitation-codes/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"));
    }

}