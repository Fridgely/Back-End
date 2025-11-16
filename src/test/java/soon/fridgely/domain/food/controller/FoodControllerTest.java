package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.food.controller.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.global.support.annotation.TestLoginMember;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/refrigerators/{refrigeratorId}/foods";

    @TestLoginMember
    @Test
    void 음식을_등록한다() throws Exception {
        // given
        var request = new FoodCreateRequest(
            "foodName",
            1L,
            BigDecimal.ONE,
            Unit.KG,
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            "description"
        );

        MockMultipartFile jsonRequest = new MockMultipartFile(
            "request",
            "request.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image-bytes".getBytes()
        );

        // expected
        mockMvc.perform(
                multipart(HttpMethod.POST, BASE_URL, 1L)
                    .file(jsonRequest)
                    .file(imageFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @ParameterizedTest
    @MethodSource("provideInvalidFoodCreateRequests")
    void 식품_등록_요청_시_필수값이_누락되면_예외가_발생한다(FoodCreateRequest request, String field, String message) throws Exception {
        // given
        MockMultipartFile jsonRequest = new MockMultipartFile(
            "request",
            "request.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        // expected
        mockMvc.perform(
                multipart(HttpMethod.POST, BASE_URL, 1L)
                    .file(jsonRequest)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value(ResultType.ERROR.name()))
            .andExpect(jsonPath("$.error.message").value(ErrorType.INVALID_REQUEST.getMessage()))
            .andExpect(jsonPath("$.error.data.%s", field).value(message));
    }

    private static Stream<Arguments> provideInvalidFoodCreateRequests() {
        return Stream.of(
            Arguments.of(
                new FoodCreateRequest(null, 1L, BigDecimal.ONE, Unit.PIECE, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "name", "음식 이름은 필수입니다."
            ),
            Arguments.of(
                new FoodCreateRequest("", 1L, BigDecimal.ONE, Unit.PIECE, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "name", "음식 이름은 필수입니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 0L, BigDecimal.ONE, Unit.PIECE, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "categoryId", "카테고리 ID는 양수여야 합니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 1L, null, Unit.PIECE, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "amount", "수량은 필수입니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 1L, new BigDecimal("-1"), Unit.PIECE, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "amount", "수량은 0 이상이어야 합니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 1L, BigDecimal.ONE, null, LocalDateTime.now().plusDays(1), StorageType.REFRIGERATION, "description"),
                "unit", "단위는 필수입니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 1L, BigDecimal.ONE, Unit.PIECE, null, StorageType.REFRIGERATION, "description"),
                "expirationDate", "유통기한은 필수입니다."
            ),
            Arguments.of(
                new FoodCreateRequest("name", 1L, BigDecimal.ONE, Unit.PIECE, LocalDateTime.now().plusDays(1), null, "description"),
                "storageType", "보관 위치는 필수입니다."
            )
        );
    }

}