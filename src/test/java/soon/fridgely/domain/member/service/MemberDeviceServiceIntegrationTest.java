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

class MemberDeviceServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberDeviceService memberDeviceService;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(member(fixtureMonkey).sample());
    }

    @Test
    void 새로운_토큰이면_디바이스를_등록한다() {
        // given
        String token = "newDeviceToken";

        // when
        memberDeviceService.syncToken(member.getId(), token);

        // then
        MemberDevice saved = memberDeviceRepository
            .findByMemberIdAndTokenAndStatus(member.getId(), token, EntityStatus.ACTIVE)
            .orElseThrow();
        assertThat(saved.getToken()).isEqualTo(token);
    }

    @Test
    void 기존_토큰이면_마지막_사용_시간을_갱신한다() {
        // given
        String token = "existingToken";
        LocalDateTime pastTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("token", token)
                .set("lastUsedAt", pastTime)
                .sample()
        );

        // when
        memberDeviceService.syncToken(member.getId(), token);

        // then
        MemberDevice updated = memberDeviceRepository
            .findByMemberIdAndTokenAndStatus(member.getId(), token, EntityStatus.ACTIVE)
            .orElseThrow();
        assertThat(updated.getLastUsedAt()).isAfter(pastTime);
    }

    @Test
    void 존재하지_않는_회원으로_새_디바이스_등록_시_예외가_발생한다() {
        // given
        long nonExistentMemberId = Long.MAX_VALUE;

        // expected
        assertThatThrownBy(() -> memberDeviceService.syncToken(nonExistentMemberId, "anyToken"))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }
}
