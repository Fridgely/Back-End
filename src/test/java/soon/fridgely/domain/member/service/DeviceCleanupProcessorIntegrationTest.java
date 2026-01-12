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

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberDeviceFixture.memberDevice;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class DeviceCleanupProcessorIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DeviceCleanupProcessor deviceCleanupProcessor;

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
    void 벌크_삭제는_여러_디바이스를_한_번에_처리한다() {
        // given
        MemberDevice device1 = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("status", EntityStatus.ACTIVE)
                .sample()
        );
        memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("status", EntityStatus.ACTIVE)
                .sample()
        );
        memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .set("status", EntityStatus.ACTIVE)
                .sample()
        );

        List<MemberDevice> devices = memberDeviceRepository.findAllByMemberId(member.getId());
        List<Long> deviceIds = devices.stream().map(MemberDevice::getId).toList();

        // when
        deviceCleanupProcessor.bulkDelete(deviceIds);

        // then
        assertThat(memberDeviceRepository.findById(device1.getId()).orElseThrow().getStatus())
            .isEqualTo(EntityStatus.DELETED);

        List<MemberDevice> allDevices = memberDeviceRepository.findAllByMemberId(member.getId());
        assertThat(allDevices).hasSize(3)
            .allMatch(device -> device.getStatus() == EntityStatus.DELETED);
    }

    @Test
    void 대량의_디바이스를_청크_단위로_벌크_삭제할_수_있다() {
        // given 1000개의 디바이스 생성
        List<MemberDevice> devices = IntStream.range(0, 1000)
            .mapToObj(i -> memberDeviceRepository.save(
                memberDevice(fixtureMonkey, member)
                    .set("token", "test-token-" + i)  // 유니크한 토큰
                    .set("status", EntityStatus.ACTIVE)
                    .sample()
            ))
            .toList();

        List<Long> deviceIds = devices.stream()
            .map(MemberDevice::getId)
            .toList();

        // when
        deviceCleanupProcessor.bulkDelete(deviceIds);

        // then
        List<MemberDevice> deletedDevices = memberDeviceRepository.findAllByMemberId(member.getId());
        assertThat(deletedDevices).hasSize(1000)
            .allMatch(device -> device.getStatus() == EntityStatus.DELETED);
    }

    @Test
    void 단건_삭제도_정상_동작한다() {
        // given
        MemberDevice device = memberDeviceRepository.save(
            memberDevice(fixtureMonkey, member)
                .sample()
        );

        // when
        deviceCleanupProcessor.deleteDevice(device);

        // then
        MemberDevice deletedDevice = memberDeviceRepository.findById(device.getId()).orElseThrow();
        assertThat(deletedDevice.getStatus()).isEqualTo(EntityStatus.DELETED);
    }

    @Test
    void 빈_리스트로_벌크_삭제_호출시_아무_동작도_하지_않는다() {
        // when
        deviceCleanupProcessor.bulkDelete(List.of());

        // then
        assertThat(memberDeviceRepository.findAllByMemberId(member.getId())).isEmpty();
    }

}