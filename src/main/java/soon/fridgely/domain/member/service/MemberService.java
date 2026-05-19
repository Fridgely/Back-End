package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.dto.response.MemberProfileResponse;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;
import soon.fridgely.global.support.logging.SlackMarkers;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberDeviceManager memberDeviceManager;  // Task 6에서 제거
    private final ImageManager imageManager;                // Task 8에서 제거

    @Transactional
    public Member register(MemberInfo memberInfo) {
        Member member = Member.register(
            memberInfo.loginId(),
            memberInfo.password(),
            memberInfo.nickname(),
            MemberRole.MEMBER,
            passwordEncoder
        );
        try {
            return memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.DUPLICATE_LOGIN_ID);
        }
    }

    @Transactional(readOnly = true)
    public Member findById(long memberId) {
        return memberRepository.findByIdAndStatus(memberId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(long memberId) {
        return MemberProfileResponse.of(findById(memberId));
    }

    @Transactional
    public void updateProfileImage(long memberId, String newImageUrl) {
        Member member = findById(memberId);
        String oldImageUrl = member.getProfileImageUrl();
        member.updateProfileImage(newImageUrl);
        if (StringUtils.hasText(oldImageUrl) && !oldImageUrl.equals(newImageUrl)) {
            eventPublisher.publishEvent(new ImageDeleteEvent(oldImageUrl));
        }
    }

    // Task 8에서 MemberFacade로 이동 후 제거
    public void updateProfileImage(long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
        String uploadedUrl = imageManager.upload(file);
        try {
            updateProfileImage(memberId, uploadedUrl);
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    // Task 6에서 MemberDeviceService로 이전 후 제거
    public void syncToken(long memberId, String token) {
        memberDeviceManager.syncToken(memberId, token, java.time.LocalDateTime.now());
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
