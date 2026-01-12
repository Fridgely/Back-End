package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeviceCleanupProcessor {

    public static final int CHUNK_SIZE = 1000;
    private final MemberDeviceRepository memberDeviceRepository;

    /**
     * 디바이스 토큰 벌크 삭제
     */
    @Transactional
    public void bulkDelete(List<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return;
        }

        int updatedCount = memberDeviceRepository.bulkUpdateStatusToDeleted(deviceIds);

        log.debug("[DeviceCleanup] 디바이스 토큰 벌크 삭제 완료. (Count={})", updatedCount);
    }

    /**
     * 디바이스 토큰 단건 삭제
     */
    @Transactional
    public void deleteDevice(MemberDevice device) {
        memberDeviceRepository.findById(device.getId())
            .ifPresentOrElse(
                managedDevice -> {
                    managedDevice.delete();
                    log.debug("[DeviceCleanup] 디바이스 토큰 삭제 완료. (DeviceId={}, Token={})", managedDevice.getId(), managedDevice.getToken());
                },
                () -> log.warn("[DeviceCleanup] 디바이스를 찾을 수 없음. (DeviceId={})", device.getId())
            );
    }

}