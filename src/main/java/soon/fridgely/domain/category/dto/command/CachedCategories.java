package soon.fridgely.domain.category.dto.command;

import soon.fridgely.domain.category.entity.Category;

import java.util.List;

public record CachedCategories(
    long refrigeratorId,
    List<CachedCategoryInfo> categories
) {

    public static CachedCategories from(long refrigeratorId, List<Category> categories) {
        return new CachedCategories(
            refrigeratorId,
            categories.stream()
                .map(CachedCategoryInfo::from)
                .toList()
        );
    }

}