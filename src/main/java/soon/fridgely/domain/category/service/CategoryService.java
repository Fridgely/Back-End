package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.category.dto.NewCategory;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryAppender categoryAppender;

    public void appendCustomCategory(NewCategory newCategory) {
        categoryAppender.appendCustomCategory(newCategory);
    }

}