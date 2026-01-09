package soon.fridgely.domain.refrigerator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.refrigerator.dto.request.InvitationCodeJoinRequest;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;

import java.util.List;

@Tag(name = "냉장고 API", description = "냉장고 조회, 수정, 초대 코드 관리 API")
public interface RefrigeratorControllerDocs {

    @Operation(summary = "냉장고 단건 조회", description = "특정 냉장고의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 냉장고 접근 키 (INVALID_REFRIGERATOR_ACCESS_KEY)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "해당 냉장고에 대한 접근 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<RefrigeratorResponse>> findRefrigerator(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") Long refrigeratorId
    );

    @Operation(summary = "내 냉장고 목록 조회", description = "현재 로그인한 사용자의 모든 냉장고 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<List<RefrigeratorResponse>>> findAllMyRefrigerators(
        @Parameter(hidden = true) Long memberId
    );

    @Operation(summary = "냉장고 이름 수정", description = "냉장고의 이름을 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이름 누락, 유효하지 않은 접근 키)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> updateRefrigerator(
        @Parameter(description = "냉장고 수정 요청 정보") RefrigeratorUpdateRequest request,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "초대 코드 생성", description = "냉장고 공유를 위한 초대 코드를 생성합니다. 코드는 일정 시간 후 만료됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "초대 코드 생성 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 냉장고 접근 키",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "403", description = "초대 코드 생성 권한 없음",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<InvitationCodeResponse>> generateInvitationCode(
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "냉장고 ID", example = "1") long refrigeratorId
    );

    @Operation(summary = "초대 코드로 냉장고 참여", description = "초대 코드를 사용하여 다른 사용자의 냉장고에 참여합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "참여 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - INVALID_INVITATION_CODE: 유효하지 않은 초대 코드
            - EXPIRED_INVITATION_CODE: 만료된 초대 코드
            - ALREADY_JOINED_REFRIGERATOR: 이미 참여한 냉장고
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> joinByInvitationCode(
        @Parameter(description = "초대 코드 정보") InvitationCodeJoinRequest request,
        @Parameter(hidden = true) Long memberId
    );

}

