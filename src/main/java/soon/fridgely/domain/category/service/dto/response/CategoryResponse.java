package soon.fridgely.domain.category.service.dto.response;

import soon.fridgely.domain.category.entity.Category;

import java.util.List;

public record CategoryResponse(
    long id,
    String name,
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