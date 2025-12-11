package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class MemberDeviceManager {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void syncToken(long memberId, String token, LocalDateTime now) {
        memberDeviceRepository.findByMemberIdAndTokenAndStatus(memberId, token, EntityStatus.ACTIVE)
            .ifPresentOrElse(
                device -> device.refreshLastUsedAt(now),
                () -> registerNewDeviceWithFallback(memberId, token, now)
            );
    }

    private void registerNewDeviceWithFallback(long memberId, String token, LocalDateTime now) {
        try {
            registerNewDevice(memberId, token, now);
        } catch (DataIntegrityViolationException e) {
            memberDeviceRepository.findByMemberIdAndTokenAndStatus(memberId, token, EntityStatus.ACTIVE)
                .ifPresent(device -> device.refreshLastUsedAt(now));
        }
    }

    private void registerNewDevice(long memberId, String token, LocalDateTime now) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        memberDeviceRepository.saveAndFlush(MemberDevice.register(member, token, now));
    }

}