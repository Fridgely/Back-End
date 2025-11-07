package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.category.dto.AddCategory;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryAppender categoryAppender;

    public void appendCustomCategory(AddCategory addCategory) {
        categoryAppender.appendCustomCategory(addCategory);
    }

}