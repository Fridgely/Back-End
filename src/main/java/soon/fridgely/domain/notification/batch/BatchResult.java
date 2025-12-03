package soon.fridgely.domain.notification.batch;

import org.jetbrains.annotations.NotNull;

public record BatchResult(
    int submittedCount,
    long durationMillis
) {

    public static BatchResult of(int count, long durationMillis) {
        return new BatchResult(count, durationMillis);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("제출 건수: %d건, 소요 시간: %dms", submittedCount, durationMillis);
    }

}