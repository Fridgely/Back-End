package soon.fridgely.domain.category.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class CategoryValidator {

    private final CategoryRepository categoryRepository;

    public void validateNotDefaultType(Category category) {
        if (category.isDefaultType()) {
            throw new CoreException(ErrorType.CANNOT_MODIFY_DEFAULT_CATEGORY);
        }
    }

    public void validateDuplicateName(String name, Refrigerator refrigerator) {
        if (categoryRepository.existsByNameAndRefrigeratorAndStatus(name, refrigerator, EntityStatus.ACTIVE)) {
            throw new CoreException(ErrorType.DUPLICATE_CATEGORY_NAME, name);
        }
    }

}