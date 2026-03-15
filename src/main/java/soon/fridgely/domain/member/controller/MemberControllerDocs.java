package soon.fridgely.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.member.dto.request.DeviceTokenSyncRequest;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;

@Tag(name = "회원 API", description = "회원 가입 및 회원 정보 관리 API")
public interface MemberControllerDocs {

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다. 가입 시 기본 세팅(냉장고, 카테고리, 알림설정)이 자동 생성됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "회원 가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락, 유효성 검증 실패)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 사용 중인 ID (DUPLICATE_LOGIN_ID)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "429", description = "요청 횟수 초과 (IP당 분당 3회 제한)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<Long>> register(
        @Parameter(description = "회원 가입 요청 정보") MemberRegisterRequest request,
        @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(summary = "디바이스 토큰 동기화", description = "FCM 푸시 알림을 위한 디바이스 토큰을 동기화합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 동기화 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (토큰 값 누락)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> syncToken(
        @Parameter(description = "디바이스 토큰 정보") DeviceTokenSyncRequest request,
        @Parameter(hidden = true) Long memberId
    );

}

