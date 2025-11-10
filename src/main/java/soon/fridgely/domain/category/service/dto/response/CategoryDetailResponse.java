package soon.fridgely.domain.category.service.dto.response;

import soon.fridgely.domain.category.entity.Category;

public record CategoryDetailResponse(
    long id,
    String name,
    boolean isDefaultType
) {

    public static CategoryDetailResponse from(Category category) {
        return new CategoryDetailResponse(category.getId(), category.getName(), category.isDefaultType());
    }

}