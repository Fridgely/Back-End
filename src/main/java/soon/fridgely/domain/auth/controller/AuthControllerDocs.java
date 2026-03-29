package soon.fridgely.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.auth.dto.request.LoginRequest;
import soon.fridgely.domain.auth.dto.request.ReissueTokenRequest;
import soon.fridgely.global.security.dto.response.TokenResponse;

@Tag(name = "인증 API", description = "로그인, 로그아웃, 토큰 재발급 등 인증 관련 API")
public interface AuthControllerDocs {

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락, 유효성 검증 실패)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패 (아이디 또는 비밀번호 불일치)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "429", description = "요청 횟수 초과",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<TokenResponse>> login(
        @Parameter(description = "로그인 요청 정보") LoginRequest request,
        @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "429", description = "요청 횟수 초과",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<TokenResponse>> reissue(
        @Parameter(description = "토큰 재발급 요청 정보") ReissueTokenRequest request,
        @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 (Access Token 누락 또는 만료)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> logout(
        @Parameter(hidden = true) Long memberId
    );

}

