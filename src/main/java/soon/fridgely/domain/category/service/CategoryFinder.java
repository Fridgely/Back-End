package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

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

    public Category findByName(String categoryName, long refrigeratorId) {
        return categoryRepository.findByNameAndRefrigeratorIdAndStatus(
                categoryName,
                refrigeratorId,
                EntityStatus.ACTIVE
            )
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Cacheable(value = "categories", key = "#refrigeratorId")
    public List<Category> findAll(long refrigeratorId) {
        return categoryRepository.findAllByRefrigeratorIdAndStatus(
            refrigeratorId,
            EntityStatus.ACTIVE
        );
    }

}