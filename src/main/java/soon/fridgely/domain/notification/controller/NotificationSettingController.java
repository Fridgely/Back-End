package soon.fridgely.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.service.NotificationSettingService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications/settings")
@RestController
public class NotificationSettingController implements NotificationSettingControllerDocs {

    private final NotificationSettingService notificationSettingService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingDetailResponse>> findNotificationSetting(
        @LoginMember Long memberId
    ) {
        NotificationSettingDetailResponse response = notificationSettingService.findNotificationSetting(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PatchMapping
    public ResponseEntity<ApiResponse<?>> updateNotificationSetting(
        @RequestBody @Valid NotificationSettingUpdateRequest request,
        @LoginMember Long memberId
    ) {
        notificationSettingService.updateNotificationSetting(memberId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

}