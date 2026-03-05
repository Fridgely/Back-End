package soon.fridgely.domain.category.dto.command;

import soon.fridgely.domain.category.entity.Category;

public record CachedCategoryInfo(
    long id,
    String name,
    boolean isDefaultType
) {

    public static CachedCategoryInfo from(Category category) {
        return new CachedCategoryInfo(
            category.getId(),
            category.getName(),
            category.isDefaultType()
        );
    }

}