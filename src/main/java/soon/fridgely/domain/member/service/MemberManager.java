package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;

@RequiredArgsConstructor
@Component
public class MemberManager {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Member register(MemberInfo memberInfo) {
        Member member = Member.register(memberInfo.loginId(),
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

    @Transactional
    public void updateProfileImage(long memberId, String newImageUrl) {
        Member member = memberRepository.findByIdAndStatus(memberId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        String oldImageUrl = member.getProfileImageUrl();

        member.updateProfileImage(newImageUrl);

        if (StringUtils.hasText(oldImageUrl)) {
            eventPublisher.publishEvent(new ImageDeleteEvent(oldImageUrl));
        }
    }

}