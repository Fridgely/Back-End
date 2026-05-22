package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class MemberDeviceService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberDeviceAppender memberDeviceAppender;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void syncToken(long memberId, String token) {
        LocalDateTime now = LocalDateTime.now();
        memberDeviceRepository.findByMemberIdAndTokenAndStatus(memberId, token, EntityStatus.ACTIVE)
            .ifPresentOrElse(
                device -> device.refreshLastUsedAt(now),
                () -> registerNewDeviceWithFallback(memberId, token, now)
            );
    }

    private void registerNewDeviceWithFallback(long memberId, String token, LocalDateTime now) {
        try {
            memberDeviceAppender.registerNewDevice(memberId, token, now);
        } catch (DataIntegrityViolationException e) {
            memberDeviceRepository.findByMemberIdAndTokenAndStatus(memberId, token, EntityStatus.ACTIVE)
                .ifPresent(device -> device.refreshLastUsedAt(now));
        }
    }
}
