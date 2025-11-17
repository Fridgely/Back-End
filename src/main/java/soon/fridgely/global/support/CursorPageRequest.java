package soon.fridgely.global.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record CursorPageRequest(
    Long cursorId,
    Integer size
) {

    private static final int DEFAULT_SIZE = 10;
    private static final long FIRST_PAGE_CURSOR_ID = Long.MAX_VALUE;

    public CursorPageRequest {
        if (size == null || size <= 0) {
            size = DEFAULT_SIZE;
        }
    }

    public long getCursorId() {
        return (cursorId == null) ? FIRST_PAGE_CURSOR_ID : cursorId;
    }

    public Pageable toPageable() {
        return PageRequest.of(0, this.size, Sort.by(Sort.Direction.DESC, "id"));
    }

}