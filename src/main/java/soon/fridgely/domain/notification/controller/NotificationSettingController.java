package soon.fridgely.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.service.NotificationSettingService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications/settings")
@RestController
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingDetailResponse>> findNotificationSetting(
        @LoginMember Long memberId
    ) {
        NotificationSettingDetailResponse response = notificationSettingService.findNotificationSetting(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}