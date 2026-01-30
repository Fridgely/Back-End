package soon.fridgely.domain.food.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.StorageType;

/**
 * Food 도메인 전용 커서 기반 페이징 요청
 * TODO: CursorPageRequest 와 중복 코드 제거 방안 고민
 */
@Schema(description = "식재료 커서 기반 페이징 요청")
public record FoodCursorPageRequest(

    @Schema(description = "커서 ID (마지막으로 조회한 항목의 ID)", example = "10")
    Long cursorId,

    @Schema(description = "페이지 크기", example = "10")
    @Positive
    Integer size,

    @Schema(description = "정렬 기준 (EXPIRATION: 유통기한 임박순, CREATED: 등록순, NAME: 이름순)", example = "EXPIRATION")
    FoodSortType sortBy,

    @Schema(description = "저장 위치 필터 (FROZEN: 냉동, REFRIGERATION: 냉장, ROOM_TEMPERATURE: 상온)", example = "REFRIGERATION")
    StorageType storageType

) {

    private static final int DEFAULT_SIZE = 10;
    private static final long FIRST_PAGE_CURSOR_ID = Long.MAX_VALUE;

    public FoodCursorPageRequest {
        if (size == null || size <= 0) {
            size = DEFAULT_SIZE;
        }
    }

    public long getCursorId() {
        return (cursorId == null) ? FIRST_PAGE_CURSOR_ID : cursorId;
    }

    /**
     * 기본 정렬(id DESC)로 Pageable 생성
     * 실제 정렬은 Repository 쿼리에서 처리
     */
    public Pageable toPageable() {
        return PageRequest.of(0, this.size, Sort.by(Sort.Direction.DESC, "id"));
    }

    public FoodSortType getSortBy() {
        return sortBy != null ? sortBy : FoodSortType.EXPIRATION;
    }

}