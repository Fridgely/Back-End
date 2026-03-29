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
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.member.dto.request.DeviceTokenSyncRequest;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;
import soon.fridgely.domain.member.dto.response.MemberProfileResponse;

@Tag(name = "회원 API", description = "회원 가입 및 회원 정보 관리 API")
public interface MemberControllerDocs {

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다. 가입 시 기본 세팅(냉장고, 카테고리, 알림설정)이 자동 생성됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "회원 가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락, 유효성 검증 실패)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 사용 중인 ID (DUPLICATE_LOGIN_ID)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "429", description = "요청 횟수 초과 (요율 제한)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<Long>> register(
        @Parameter(description = "회원 가입 요청 정보") MemberRegisterRequest request,
        @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(summary = "마이페이지 조회", description = "현재 로그인한 회원의 프로필 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<MemberProfileResponse>> getMyProfile(
        @Parameter(hidden = true) Long memberId
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

    @Operation(summary = "프로필 사진 업로드/수정", description = "회원의 프로필 사진을 업로드하거나 기존 사진을 교체합니다. 최대 10MB, jpg/jpeg/png/gif/webp 형식 지원.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "프로필 사진 업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "파일 크기 초과 또는 허용되지 않은 파일 형식 (FILE_SIZE_EXCEEDED, INVALID_FILE_TYPE)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "500", description = "파일 업로드 실패 (STORAGE_UPLOAD_FAILED)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> updateProfileImage(
        @Parameter(description = "프로필 사진 파일 (최대 10MB, jpg/jpeg/png/gif/webp)") MultipartFile file,
        @Parameter(hidden = true) Long memberId
    );

}
