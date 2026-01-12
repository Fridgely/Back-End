package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberDeviceFixture.memberDevice;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class DeviceCleanupSchedulerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DeviceCleanupScheduler deviceCleanupScheduler;

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
    void 장기_미사용_디바이스를_삭제_상태로_변경한다() {
        // given
        LocalDateTime oldDate = LocalDateTime.now().minusDays(120);
        MemberDevice oldDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", oldDate)
                .sample()
        );

        // when
        BatchResult result = deviceCleanupScheduler.cleanupInactiveDevices();

        // then
        assertThat(result.submittedCount()).isEqualTo(1);

        MemberDevice deletedDevice = memberDeviceRepository.findById(oldDevice.getId()).orElseThrow();
        assertThat(deletedDevice.getStatus()).isEqualTo(EntityStatus.DELETED);
    }

    @Test
    void 최근_사용된_디바이스는_삭제하지_않는다() {
        // given
        LocalDateTime recentDate = LocalDateTime.now().minusDays(10);

        MemberDevice recentDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", recentDate)
                .sample()
        );

        // when
        BatchResult result = deviceCleanupScheduler.cleanupInactiveDevices();

        // then
        assertThat(result.submittedCount()).isZero();

        MemberDevice device = memberDeviceRepository.findById(recentDevice.getId()).orElseThrow();
        assertThat(device.getStatus()).isEqualTo(EntityStatus.ACTIVE);
    }

    @Test
    void 정확히_90일_이상_미사용된_디바이스만_삭제한다() {
        // given
        LocalDateTime exactly90DaysAgo = LocalDateTime.now().minusDays(90).minusHours(1);
        LocalDateTime exactly89DaysAgo = LocalDateTime.now().minusDays(89);

        MemberDevice oldDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", exactly90DaysAgo)
                .sample()
        );
        MemberDevice recentDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", exactly89DaysAgo)
                .sample()
        );

        // when
        BatchResult result = deviceCleanupScheduler.cleanupInactiveDevices();

        // then
        assertThat(result.submittedCount()).isEqualTo(1);

        MemberDevice deletedDevice = memberDeviceRepository.findById(oldDevice.getId()).orElseThrow();
        assertThat(deletedDevice.getStatus()).isEqualTo(EntityStatus.DELETED);

        MemberDevice activeDevice = memberDeviceRepository.findById(recentDevice.getId()).orElseThrow();
        assertThat(activeDevice.getStatus()).isEqualTo(EntityStatus.ACTIVE);
    }

}