package soon.fridgely.global.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.util.StopWatch;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.global.support.CursorPageRequest;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 커서 기반 페이징 배치 처리 추상 클래스
 */
@Slf4j
public abstract class AbstractBatchExecutor<T> {

    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 배치 처리 실행
     */
    protected BatchResult execute(
        Function<CursorPageRequest, Slice<T>> fetcher,
        Consumer<T> task,
        String batchName
    ) {
        return execute(fetcher, task, batchName, DEFAULT_BATCH_SIZE);
    }

    /**
     * 배치 처리 실행 (배치 사이즈 지정)
     */
    protected BatchResult execute(
        Function<CursorPageRequest, Slice<T>> fetcher,
        Consumer<T> task,
        String batchName,
        int batchSize
    ) {
        StopWatch stopWatch = new StopWatch(batchName);
        stopWatch.start();

        Long cursorId = null;  // 처음엔 null로 시작 (전체 조회)
        int totalProcessed = 0;

        while (true) {
            CursorPageRequest cursorRequest = new CursorPageRequest(cursorId, batchSize);
            Slice<T> slice = fetcher.apply(cursorRequest);

            if (slice.isEmpty()) {
                break;
            }

            // 각 엔티티 처리 실패해도 나머지는 계속 진행
            for (T entity : slice.getContent()) {
                try {
                    task.accept(entity);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("[{}] 배치 처리 중 오류 발생. (Entity={})", batchName, entity, e);
                }
            }

            if (!slice.hasNext()) {
                break;
            }

            // 다음 페이지 조회를 위해 마지막 ID를 커서로 사용
            int fetched = slice.getNumberOfElements();
            cursorId = getEntityId(slice.getContent().get(fetched - 1));
        }

        stopWatch.stop();
        log.info("[{}] 배치 처리 완료. (ProcessedCount={}, DurationMs={})", batchName, totalProcessed, stopWatch.getTotalTimeMillis());

        return BatchResult.of(totalProcessed, stopWatch.getTotalTimeMillis());
    }

    /**
     * 엔티티 ID 추출
     */
    protected abstract Long getEntityId(T entity);

}