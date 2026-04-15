package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.MemberDeviceFixture.memberDevice;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class MemberDeviceManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberDeviceManager memberDeviceManager;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
    }

    @Test
    void 새로운_토큰이면_디바이스를_등록한다() {
        // given
        String token = "newDeviceToken";

        // when
        memberDeviceManager.syncToken(member.getId(), token, LocalDateTime.now());

        // then
        MemberDevice memberDevice = memberDeviceRepository.findByMemberIdAndTokenAndStatus(member.getId(), token, EntityStatus.ACTIVE).orElseThrow();
        assertThat(memberDevice).isNotNull()
            .extracting(MemberDevice::getToken)
            .isEqualTo(token);
    }

    @Test
    void 기존_토큰이면_마지막_사용_시간을_갱신한다() {
        // given
        String token = "existingToken";
        LocalDateTime pastTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        MemberDevice device = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("token", token)
                .set("lastUsedAt", pastTime)
                .sample()
        );
        LocalDateTime originalLastUsedAt = device.getLastUsedAt();

        // when
        memberDeviceManager.syncToken(member.getId(), token, LocalDateTime.now());

        // then
        MemberDevice updatedDevice = memberDeviceRepository.findByMemberIdAndTokenAndStatus(member.getId(), token, EntityStatus.ACTIVE).orElseThrow();
        assertThat(updatedDevice.getLastUsedAt()).isAfter(originalLastUsedAt);
    }

    @Test
    void 존재하지_않는_회원으로_새_디바이스_등록_시_예외가_발생한다() {
        // given
        long nonExistentMemberId = Long.MAX_VALUE;
        String token = "newToken";

        // expected
        assertThatThrownBy(() -> memberDeviceManager.syncToken(nonExistentMemberId, token, LocalDateTime.now()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

}