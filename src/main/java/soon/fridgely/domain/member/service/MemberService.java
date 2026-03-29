package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.dto.response.MemberProfileResponse;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.logging.SlackMarkers;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberManager memberManager;
    private final ImageManager imageManager;
    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;
    private final NotificationSettingManager notificationSettingManager;
    private final MemberDeviceManager memberDeviceManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원 가입 후 기본 냉장고 및 알림 설정 생성 후 기본 카테고리 생성 이벤트 발행
     *
     * @param memberInfo 회원 정보
     * @return 생성된 회원 ID
     */
    @Transactional
    public Long register(MemberInfo memberInfo) {
        Member member = memberManager.register(memberInfo);
        notificationSettingManager.createDefaultSetting(member);

        Refrigerator refrigerator = refrigeratorManager.register(member);
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // 냉장고 생성 완료 후 Default category 생성
        eventPublisher.publishEvent(new RefrigeratorCreatedEvent(refrigerator.getId(), member.getId()));

        return member.getId();
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(long memberId) {
        Member member = memberManager.findById(memberId);
        return MemberProfileResponse.of(member);
    }

    public void syncToken(long memberId, String token) {
        LocalDateTime now = LocalDateTime.now();
        memberDeviceManager.syncToken(memberId, token, now);
    }

    public void updateProfileImage(long memberId, MultipartFile file) {
        String uploadedUrl = imageManager.upload(file);

        try {
            memberManager.updateProfileImage(memberId, uploadedUrl);
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    private void rollbackImageUpload(String imageUrl) {
        if (imageUrl != null) {
            try {
                imageManager.delete(imageUrl);
            } catch (Exception e) {
                log.warn(SlackMarkers.SYSTEM, "[Member] 이미지 롤백 실패 - 수동 정리 필요 (ImageUrl={})", imageUrl, e);
            }
        }
    }

}