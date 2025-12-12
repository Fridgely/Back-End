package soon.fridgely.domain.food.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodConditionResponse;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.QuantityResponse;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StockActionType;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.ResultType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @TestLoginMember
    @Test
    void 이미지가_포함된_음식을_수정한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long foodId = 1L;

        var request = new FoodUpdateRequest(
            "수정된 이름",
            2L,
            BigDecimal.TEN,
            Unit.KG,
            LocalDateTime.now().plusDays(10),
            StorageType.ROOM_TEMPERATURE,
            "수정된 설명"
        );

        MockMultipartFile jsonRequest = new MockMultipartFile(
            "request",
            "request.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new-image-bytes".getBytes()
        );

        // expected
        mockMvc.perform(
                multipart(HttpMethod.PATCH, BASE_URL + "/{foodId}", refrigeratorId, foodId)
                    .file(jsonRequest)
                    .file(imageFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 이미지가_포함되지_않은_음식을_수정한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long foodId = 1L;

        var request = new FoodUpdateRequest(
            "수정된 이름",
            1L,
            BigDecimal.ONE,
            Unit.KG,
            LocalDateTime.now(),
            StorageType.FROZEN,
            "설명"
        );

        MockMultipartFile jsonRequest = new MockMultipartFile(
            "request",
            "request.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        // expected
        mockMvc.perform(
                multipart(HttpMethod.PATCH, BASE_URL + "/{foodId}", refrigeratorId, foodId)
                    .file(jsonRequest)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 음식을_조회한다() throws Exception {
        // given
        var response = new FoodDetailResponse(
            1L,
            "foodName",
            "categoryName",
            new QuantityResponse(BigDecimal.ONE, Unit.KG),
            new FoodConditionResponse(LocalDateTime.now().plusDays(2L), StorageType.FROZEN, FoodStatus.GREEN, 1L),
            "description",
            "http://example.com/image.jpg"
        );

        given(foodService.findFood(anyLong(), any(MemberRefrigeratorKey.class)))
            .willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/{foodId}", 1L, 1L)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(response.id()))
            .andExpect(jsonPath("$.data.name").value(response.name()))
            .andExpect(jsonPath("$.data.categoryName").value(response.categoryName()))
            .andExpect(jsonPath("$.data.quantity.amount").value(response.quantity().amount()))
            .andExpect(jsonPath("$.data.quantity.unit").value(response.quantity().unit().name()))
            .andExpect(jsonPath("$.data.condition.storageType").value(response.condition().storageType().name()))
            .andExpect(jsonPath("$.data.condition.foodStatus").value(response.condition().foodStatus().name()));
    }

    @TestLoginMember
    @Test
    void 음식_목록을_조회한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        int size = 10;

        var foodResponse = new FoodResponse(
            1L,
            "Test Food",
            "Test Category",
            "http://example.com/image.jpg",
            new QuantityResponse(new BigDecimal("2.5"), Unit.KG),
            new FoodConditionResponse(LocalDateTime.now(), StorageType.REFRIGERATION, FoodStatus.GREEN, 2L)
        );

        List<FoodResponse> content = List.of(foodResponse);
        Pageable pageable = PageRequest.of(0, size);
        Slice<FoodResponse> mockSlice = new SliceImpl<>(content, pageable, true);

        given(foodService.findAllFoods(any(MemberRefrigeratorKey.class), any(CursorPageRequest.class)))
            .willReturn(mockSlice);

        // expected
        mockMvc.perform(
                get(BASE_URL, refrigeratorId)
                    .param("size", String.valueOf(size))
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.last").value(false))
            .andExpect(jsonPath("$.data.numberOfElements").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(foodResponse.id()))
            .andExpect(jsonPath("$.data.content[0].name").value(foodResponse.name()))
            .andExpect(jsonPath("$.data.content[0].imageURL").value(foodResponse.imageURL()));
    }

    @TestLoginMember
    @Test
    void 음식을_삭제한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long foodId = 1L;

        // expected
        mockMvc.perform(
                delete(BASE_URL + "/{foodId}", refrigeratorId, foodId)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @Test
    void 식재료_재고를_조정한다() throws Exception {
        // given
        long refrigeratorId = 1L;
        long foodId = 1L;

        var request = new FoodStockUpdateRequest(
            BigDecimal.ONE,
            Unit.PIECE,
            StockActionType.CONSUME
        );

        // expected
        mockMvc.perform(
                patch(BASE_URL + "/{foodId}/stock", refrigeratorId, foodId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @TestLoginMember
    @ParameterizedTest
    @MethodSource("provideInvalidFoodStockUpdateRequests")
    void 식재료_재고_조정_요청_시_필수값이_누락되면_예외가_발생한다(FoodStockUpdateRequest request, String field, String message) throws Exception {
        // given
        long refrigeratorId = 1L;
        long foodId = 1L;

        // expected
        mockMvc.perform(
                patch(BASE_URL + "/{foodId}/stock", refrigeratorId, foodId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
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

    private static Stream<Arguments> provideInvalidFoodStockUpdateRequests() {
        return Stream.of(
            Arguments.of(
                new FoodStockUpdateRequest(null, Unit.PIECE, StockActionType.ADD),
                "amount", "수량은 필수입니다."
            ),
            Arguments.of(
                new FoodStockUpdateRequest(BigDecimal.ONE, null, StockActionType.ADD),
                "unit", "단위는 필수입니다."
            ),
            Arguments.of(
                new FoodStockUpdateRequest(BigDecimal.ONE, Unit.PIECE, null),
                "action", "변경 유형은 필수입니다."
            )
        );
    }

}