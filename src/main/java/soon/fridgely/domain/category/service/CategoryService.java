package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.category.dto.AddCategory;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryAppender categoryAppender;
    private final RefrigeratorAccessValidator refrigeratorAccessValidator;

    public void appendCustomCategory(AddCategory addCategory) {
        refrigeratorAccessValidator.validateMembership(addCategory.refrigeratorId(), addCategory.memberId());
        categoryAppender.appendCustomCategory(addCategory);
    }

}