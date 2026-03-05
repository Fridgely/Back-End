package soon.fridgely.domain.food.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;

@Tag(name = "내 식재료 API", description = "사용자의 전체 냉장고 식재료 상태 조회 API")
public interface MyFoodControllerDocs {

    @Operation(summary = "상태별 식재료 조회",
        description = """
            현재 로그인한 사용자의 모든 냉장고에서 식재료를 유통기한 상태별로 그룹화하여 조회합니다.
            
            **상태 분류 기준:**
            - BLACK: 유통기한 만료
            - RED: 유통기한 10일 이내
            - YELLOW: 유통기한 20일 이내
            - GREEN: 유통기한 양호
            
            **응답 정보:**
            - 각 상태별 식재료 목록과 함께 개수(blackCount, redCount, yellowCount, greenCount)를 제공합니다.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<FoodStatusResponse>> findAllMyFoodsGroupedByStatus(
        @Parameter(hidden = true) Long memberId
    );

}

