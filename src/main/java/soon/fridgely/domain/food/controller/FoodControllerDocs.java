package soon.fridgely.domain.food.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodCursorPageRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;

@Tag(name = "식재료 API", description = "냉장고 내 식재료 관리 API")
public interface FoodControllerDocs {

    @Operation(summary = "식재료 등록", description = "냉장고에 새로운 식재료를 등록합니다. 이미지 파일 업로드가 가능합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "등록 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - 필수 값 누락 또는 유효성 검증 실패
            - INVALID_REFRIGERATOR_ACCESS_KEY: 유효하지 않은 냉장고 접근 키
            - FILE_SIZE_EXCEEDED: 파일 크기 초과 (최대 10MB)
            - INVALID_FILE_TYPE: 허용되지 않은 파일 형식
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "500", description = "STORAGE_UPLOAD_FAILED: 파일 업로드 실패",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> createFood(
        @Parameter(description = "식재료 정보") FoodCreateRequest request,
        @Parameter(description = "식재료 이미지 파일 (최대 10MB, jpg/png/gif)") MultipartFile image,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "식재료 수정", description = "기존 식재료 정보를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - 필수 값 누락 또는 유효성 검증 실패
            - NOT_FOUND_DATA: 해당 식재료를 찾을 수 없음
            - FILE_SIZE_EXCEEDED: 파일 크기 초과
            - INVALID_FILE_TYPE: 허용되지 않은 파일 형식
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> updateFood(
        @Parameter(description = "식재료 수정 정보") FoodUpdateRequest request,
        @Parameter(description = "식재료 이미지 파일") MultipartFile image,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "식재료 ID", example = "1") long foodId
    );

    @Operation(summary = "식재료 재고 변경", description = "식재료의 수량을 증감합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재고 변경 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - 필수 값 누락 또는 유효성 검증 실패
            - NOT_FOUND_DATA: 해당 식재료를 찾을 수 없음
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "CONCURRENT_UPDATE_LIMIT_EXCEEDED: 동시 요청 충돌",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> updateFoodStock(
        @Parameter(description = "재고 변경 정보") FoodStockUpdateRequest request,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "식재료 ID", example = "1") long foodId
    );

    @Operation(summary = "식재료 상세 조회", description = "특정 식재료의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "NOT_FOUND_DATA: 해당 식재료를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<FoodDetailResponse>> findFood(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "식재료 ID", example = "1") long foodId
    );

    @Operation(summary = "식재료 목록 조회",
        description = """
            냉장고 내 모든 식재료를 커서 기반 페이지네이션으로 조회합니다.
            
            **정렬 기준:**
            - EXPIRATION: 유통기한 임박순 (기본값)
            - CREATED: 등록순 (최신순) - 가장 안정적인 정렬
            - NAME: 이름순 (가나다순)
            
            **저장 위치 필터:**
            - FROZEN: 냉동 보관 식재료만 조회
            - REFRIGERATION: 냉장 보관 식재료만 조회
            - ROOM_TEMPERATURE: 상온 보관 식재료만 조회
            - 지정하지 않으면 모든 저장 위치의 식재료를 조회
            
            **주의사항:**
            - 정렬 조건 변경 시 cursorId를 null로 설정하여 첫 페이지부터 다시 요청해주세요.
            - 안정적인 페이지네이션을 위해서는 CREATED(등록순) 정렬을 권장합니다.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 냉장고 접근 키",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<Slice<FoodResponse>>> findAllFoods(
        @Parameter(hidden = true) Long memberId,
        @ModelAttribute @Parameter(description = "커서 기반 페이징 정보") FoodCursorPageRequest cursorRequest,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "식재료 삭제", description = "식재료를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "NOT_FOUND_DATA: 해당 식재료를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> deleteFood(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "식재료 ID", example = "1") long foodId
    );

}