package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class CategoryFinder {

    private final CategoryRepository categoryRepository;

    public Category findByRefrigerator(long categoryId, long refrigeratorId) {
        return categoryRepository.findByIdAndRefrigeratorIdAndStatus(
                categoryId,
                refrigeratorId,
                EntityStatus.ACTIVE
            )
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

}