package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MemberDeviceManagerUnitTest {

    @InjectMocks
    private MemberDeviceManager memberDeviceManager;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    void 동시_요청으로_DataIntegrityViolationException_발생_시_기존_디바이스의_마지막_사용_시간을_갱신한다() {
        // given
        long memberId = 1L;
        String token = "duplicateToken";
        LocalDateTime now = LocalDateTime.now();

        MemberDevice existingDevice = mock(MemberDevice.class);
        Member mockMember = mock(Member.class);

        // 첫 조회: 없음 → 신규 등록 시도 / 두 번째 조회(fallback): 다른 스레드가 먼저 등록한 디바이스 반환
        given(memberDeviceRepository.findByMemberIdAndTokenAndStatus(memberId, token, EntityStatus.ACTIVE))
            .willReturn(Optional.empty())
            .willReturn(Optional.of(existingDevice));

        given(memberRepository.findById(memberId)).willReturn(Optional.of(mockMember));
        given(memberDeviceRepository.saveAndFlush(any()))
            .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when
        memberDeviceManager.syncToken(memberId, token, now);

        // then - fallback에서 기존 디바이스의 마지막 사용 시간 갱신
        then(existingDevice).should().refreshLastUsedAt(now);
    }

}