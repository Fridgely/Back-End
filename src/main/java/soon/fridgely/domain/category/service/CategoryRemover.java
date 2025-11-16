package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.validator.CategoryValidator;

@RequiredArgsConstructor
@Component
public class CategoryRemover {

    private final CategoryValidator categoryValidator;
    private final CategoryFinder categoryFinder;

    @Transactional
    public void remove(DeleteCategory deleteCategory) {
        Category category = categoryFinder.findByRefrigerator(deleteCategory.categoryId(), deleteCategory.refrigeratorId());
        categoryValidator.validateNotDefaultType(category);
        category.delete();
    }

}