package soon.fridgely.domain.member.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.batch.AbstractBatchExecutor;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeviceCleanupBatchExecutor extends AbstractBatchExecutor<MemberDevice> {

    private static final int BATCH_SIZE = 100;
    private final MemberDeviceRepository memberDeviceRepository;

    /**
     * 장기 미사용 디바이스 토큰 정리 배치 실행
     */
    public BatchResult executeCleanup(LocalDateTime threshold, Consumer<MemberDevice> task) {
        return execute(
            cursorRequest -> memberDeviceRepository.findInactiveDevices(
                EntityStatus.ACTIVE,
                threshold,
                cursorRequest.getCursorId(),
                cursorRequest.toPageable()
            ),
            task,
            "Device Cleanup Batch",
            BATCH_SIZE
        );
    }

    @Override
    protected Long getEntityId(MemberDevice device) {
        return device.getId();
    }

}