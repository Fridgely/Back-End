package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class MemberDeviceAppender {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerNewDevice(long memberId, String token, LocalDateTime now) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        memberDeviceRepository.save(MemberDevice.register(member, token, now));
    }

}