package soon.fridgely.domain.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.category.entity.Category;

@Schema(description = "카테고리 상세 응답")
public record CategoryDetailResponse(

    @Schema(description = "카테고리 ID", example = "1")
    long id,

    @Schema(description = "카테고리 이름", example = "유제품")
    String name,

    @Schema(description = "기본 카테고리 여부", example = "true")
    boolean isDefaultType

) {

    public static CategoryDetailResponse from(Category category) {
        return new CategoryDetailResponse(category.getId(), category.getName(), category.isDefaultType());
    }

}