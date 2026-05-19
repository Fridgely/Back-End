package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.RefrigeratorService;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.logging.SlackMarkers;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberFacade {

    private final MemberService memberService;
    private final RefrigeratorService refrigeratorService;
    private final NotificationSettingManager notificationSettingManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageManager imageManager;

    @Transactional
    public Long register(MemberInfo memberInfo) {
        Member member = memberService.register(memberInfo);
        notificationSettingManager.createDefaultSetting(member);
        Refrigerator refrigerator = refrigeratorService.register(member);
        refrigeratorService.linkToOwner(member, refrigerator);
        eventPublisher.publishEvent(new RefrigeratorCreatedEvent(refrigerator.getId(), member.getId()));
        return member.getId();
    }

    public void updateProfileImage(long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }

        String uploadedUrl = imageManager.upload(file);

        try {
            memberService.updateProfileImage(memberId, uploadedUrl);
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
