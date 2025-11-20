package soon.fridgely.domain.refrigerator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.InvitationCodeJoinRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.global.security.annotation.TestLoginMember;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefrigeratorControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/refrigerators";

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