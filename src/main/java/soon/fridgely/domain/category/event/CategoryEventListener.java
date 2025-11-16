package soon.fridgely.domain.category.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import soon.fridgely.domain.category.service.CategoryAppender;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;

@Slf4j
@RequiredArgsConstructor
@Component
public class CategoryEventListener {

    private final CategoryAppender categoryAppender;

    /**
     * MemberService.register()의 @Transactional이 성공적으로 커밋된 이후에만 실행.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRefrigeratorCreated(RefrigeratorCreatedEvent event) {
        try {
            categoryAppender.appendDefaultCategories(event.toKey());
            log.info("냉장고({}) 기본 카테고리 생성 완료.", event.refrigeratorId());
        } catch (Exception e) {
            log.error("냉장고({}) 기본 카테고리 생성 중 오류 발생", event.refrigeratorId(), e);
        }
    }

}