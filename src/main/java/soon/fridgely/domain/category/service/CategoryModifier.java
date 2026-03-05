package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.validator.CategoryValidator;

@RequiredArgsConstructor
@Component
public class CategoryModifier {

    private final CategoryFinder categoryFinder;
    private final CategoryValidator categoryValidator;

    @CacheEvict(value = "categories", key = "#modifyCategory.refrigeratorId()")
    @Transactional
    public void modify(ModifyCategory modifyCategory) {
        Category category = categoryFinder.findByRefrigerator(modifyCategory.categoryId(), modifyCategory.refrigeratorId());

        if (category.isSameName(modifyCategory.newName())) {
            return;
        }
        categoryValidator.validateNotDefaultType(category);
        categoryValidator.validateDuplicateName(modifyCategory.newName(), category.getRefrigerator());

        category.updateName(modifyCategory.newName());
    }

}