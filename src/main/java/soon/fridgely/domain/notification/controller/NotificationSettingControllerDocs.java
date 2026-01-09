package soon.fridgely.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;

@Tag(name = "알림 설정 API", description = "유통기한 알림 설정 관리 API")
public interface NotificationSettingControllerDocs {

    @Operation(summary = "알림 설정 조회", description = "현재 로그인한 사용자의 알림 설정을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 (Access Token 누락 또는 만료)",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<NotificationSettingDetailResponse>> findNotificationSetting(
        @Parameter(hidden = true) Long memberId
    );

    @Operation(summary = "알림 설정 수정",
        description = """
            알림 시간, 유통기한 기준일, 활성화 여부를 수정합니다.
            
            **설정 항목:**
            - notificationTime: 알림을 받을 시간 (예: 09:00:00)
            - daysBeforeExpiration: 유통기한 며칠 전에 알림을 받을지 (1~30일)
            - enabled: 알림 활성화 여부
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = """
            잘못된 요청:
            - 필수 값 누락 (알림 시간, 기준일, 활성화 여부)
            - daysBeforeExpiration: 1~30 범위를 벗어남
            """,
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = soon.fridgely.global.support.response.ApiResponse.class)))
    })
    ResponseEntity<soon.fridgely.global.support.response.ApiResponse<?>> updateNotificationSetting(
        @Parameter(description = "알림 설정 수정 요청 정보") NotificationSettingUpdateRequest request,
        @Parameter(hidden = true) Long memberId
    );

}

