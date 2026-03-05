package soon.fridgely.domain.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.category.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.dto.request.CategoryModifyRequest;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;

import java.util.List;

@Tag(name = "카테고리 API", description = "냉장고 내 식재료 카테고리 관리 API")
public interface CategoryControllerDocs {

    @Operation(summary = "카테고리 추가", description = "냉장고에 새로운 커스텀 카테고리를 추가합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "카테고리 추가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (카테고리 이름 누락)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "DUPLICATE_CATEGORY_NAME: 이미 존재하는 카테고리 이름",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> append(
        @Parameter(description = "카테고리 추가 요청 정보") CategoryAddRequest request,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "NOT_FOUND_DATA: 카테고리를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<CategoryDetailResponse>> find(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "카테고리 ID", example = "1") long categoryId
    );

    @Operation(summary = "카테고리 목록 조회", description = "냉장고 내 모든 카테고리(기본 + 커스텀)를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 냉장고 접근 키",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<List<CategoryResponse>>> findAll(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "카테고리 수정", description = "커스텀 카테고리의 이름을 수정합니다. 기본 카테고리는 수정할 수 없습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - 카테고리 이름 누락
            - CANNOT_MODIFY_DEFAULT_CATEGORY: 기본 카테고리 수정 불가
            - NOT_FOUND_DATA: 카테고리를 찾을 수 없음
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "DUPLICATE_CATEGORY_NAME: 이미 존재하는 카테고리 이름",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> modify(
        @Parameter(description = "카테고리 수정 요청 정보") CategoryModifyRequest request,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "카테고리 ID", example = "1") long categoryId
    );

    @Operation(summary = "카테고리 삭제", description = "커스텀 카테고리를 삭제합니다. 기본 카테고리는 삭제할 수 없습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - CANNOT_MODIFY_DEFAULT_CATEGORY: 기본 카테고리 삭제 불가
            - NOT_FOUND_DATA: 카테고리를 찾을 수 없음
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> remove(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId,
        @Parameter(description = "카테고리 ID", example = "1") long categoryId
    );

}

