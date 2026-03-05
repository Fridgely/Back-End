package soon.fridgely.domain.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.category.entity.Category;

import java.util.List;

@Schema(description = "카테고리 응답")
public record CategoryResponse(

    @Schema(description = "카테고리 ID", example = "1")
    long id,

    @Schema(description = "카테고리 이름", example = "유제품")
    String name,

    @Schema(description = "기본 카테고리 여부", example = "true")
    boolean isDefaultType

) {

    public static List<CategoryResponse> from(List<Category> categories) {
        return categories.stream()
            .map(category -> new CategoryResponse(
                category.getId(),
                category.getName(),
                category.isDefaultType()
            ))
            .toList();
    }

}