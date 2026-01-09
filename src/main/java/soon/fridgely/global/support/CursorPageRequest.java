package soon.fridgely.global.support;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Schema(description = "커서 기반 페이징 요청")
public record CursorPageRequest(

    @Schema(description = "커서 ID (마지막으로 조회한 항목의 ID)", example = "10")
    Long cursorId,

    @Schema(description = "페이지 크기", example = "10")
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