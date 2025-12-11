package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberDeviceManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberDeviceManager memberDeviceManager;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 새로운_토큰이면_디바이스를_등록한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);
        String token = "newDeviceToken";

        // when
        memberDeviceManager.syncToken(member.getId(), token);

        // then
        MemberDevice memberDevice = memberDeviceRepository.findByMemberIdAndToken(member.getId(), token).orElseThrow();
        assertThat(memberDevice).isNotNull()
            .extracting(MemberDevice::getToken)
            .isEqualTo(token);
    }

    @Test
    void 기존_토큰이면_마지막_사용_시간을_갱신한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);
        String token = "existingToken";

        LocalDateTime pastTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        MemberDevice memberDevice = MemberDevice.register(member, token, pastTime);
        memberDeviceRepository.save(memberDevice);
        LocalDateTime originalLastUsedAt = memberDevice.getLastUsedAt();

        // when
        memberDeviceManager.syncToken(member.getId(), token);

        // then
        MemberDevice updatedDevice = memberDeviceRepository.findByMemberIdAndToken(member.getId(), token).orElseThrow();
        assertThat(updatedDevice.getLastUsedAt()).isAfter(originalLastUsedAt);
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}