package soon.fridgely.global.support.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.ControllerTestSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiRestExceptionHandlerTest extends ControllerTestSupport {

    @TestLoginMember
    @Test
    void 파일_크기_초과_업로드_시_400과_FILE_SIZE_EXCEEDED를_반환한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        MockMultipartFile image = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", new byte[1]
        );
        MockMultipartFile request = new MockMultipartFile(
            "request", "", "application/json",
            """
            {"name":"테스트","categoryId":1,"amount":1.0,"unit":"PIECE","expirationDate":"2026-12-31T00:00:00","storageType":"REFRIGERATION"}
            """.getBytes()
        );

        willThrow(new MaxUploadSizeExceededException(10 * 1024 * 1024))
            .given(foodFacade).createFood(any(), any(), any());

        // expected
        mockMvc.perform(
                multipart("/api/v1/refrigerators/{refrigeratorId}/foods", refrigeratorId)
                    .file(image)
                    .file(request)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.error.message").value("파일 크기가 허용 범위를 초과했습니다. (최대 10MB)"));
    }
}
