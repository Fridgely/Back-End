package soon.fridgely.global.support.image.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import soon.fridgely.global.support.image.ImageManager;

/**
 * 이미지 삭제 이벤트 리스너
 * DB 트랜잭션 커밋 후 이미지 삭제를 안전하게 수행
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ImageEventListener {

    private final ImageManager imageManager;

    /**
     * 트랜잭션 커밋 후 이미지 삭제
     * DB 커밋 성공 후 S3 이미지 삭제를 시도하여 데이터 정합성 유지
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageDelete(ImageDeleteEvent event) {
        try {
            imageManager.delete(event.imageUrl());
            log.info("[ImageEvent] 이미지 삭제 완료. (ImageUrl={})", event.imageUrl());
        } catch (Exception e) {
            log.error("[ImageEvent] 이미지 삭제 실패. (ImageUrl={})", event.imageUrl(), e);
            // 이미지 삭제 실패는 DB 트랜잭션에 영향을 주지 않음
        }
    }

}