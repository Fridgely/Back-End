package soon.fridgely.domain.member.batch;

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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberDeviceFixture.memberDevice;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class DeviceCleanupBatchExecutorIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DeviceCleanupBatchExecutor deviceCleanupBatchExecutor;

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
    void 임계값_이전에_마지막으로_사용된_디바이스만_조회한다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        LocalDateTime oldDate = LocalDateTime.now().minusDays(120);
        LocalDateTime recentDate = LocalDateTime.now().minusDays(60);

        MemberDevice oldDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", oldDate)
                .sample()
        );
        memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", recentDate)
                .sample()
        );

        List<Long> processedIds = new ArrayList<>();

        // when
        BatchResult result = deviceCleanupBatchExecutor.executeCleanup(
            threshold,
            device -> processedIds.add(device.getId())
        );

        // then
        assertThat(result.submittedCount()).isEqualTo(1);
        assertThat(processedIds).containsExactly(oldDevice.getId());
    }

    @Test
    void 삭제된_디바이스는_조회하지_않는다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        LocalDateTime oldDate = LocalDateTime.now().minusDays(120);

        MemberDevice activeDevice = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", oldDate)
                .sample()
        );
        memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("lastUsedAt", oldDate)
                .set("status", EntityStatus.DELETED)
                .sample()
        );

        List<Long> processedIds = new ArrayList<>();

        // when
        BatchResult result = deviceCleanupBatchExecutor.executeCleanup(
            threshold,
            device -> processedIds.add(device.getId())
        );

        // then
        assertThat(result.submittedCount()).isEqualTo(1);
        assertThat(processedIds).containsExactly(activeDevice.getId());
    }

    @Test
    void 배치_결과에_처리_건수와_소요_시간이_포함된다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        LocalDateTime oldDate = LocalDateTime.now().minusDays(120);

        for (int i = 0; i < 5; i++) {
            memberDeviceRepository.save(
                memberDevice(fixtureMonkey, member)
                    .set("lastUsedAt", oldDate)
                    .sample()
            );
        }

        // when
        BatchResult result = deviceCleanupBatchExecutor.executeCleanup(
            threshold,
            device -> {
            }
        );

        // then
        assertThat(result.submittedCount()).isEqualTo(5);
        assertThat(result.durationMillis()).isGreaterThanOrEqualTo(0);
    }

}