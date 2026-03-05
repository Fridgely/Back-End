package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryAppender categoryAppender;
    private final CategoryFinder categoryFinder;
    private final CategoryModifier categoryModifier;
    private final CategoryRemover categoryRemover;

    @ValidateRefrigeratorAccess(key = "#addCategory.toKey()")
    public void appendCustomCategory(AddCategory addCategory) {
        categoryAppender.appendCustomCategory(addCategory);
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public CategoryDetailResponse findCategory(long categoryId, MemberRefrigeratorKey key) {
        return CategoryDetailResponse.from(categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId()));
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public List<CategoryResponse> findAllCategory(MemberRefrigeratorKey key) {
        return categoryFinder.findAll(key.refrigeratorId())
            .categories()
            .stream()
            .map(info -> new CategoryResponse(info.id(), info.name(), info.isDefaultType()))
            .toList();
    }

    @ValidateRefrigeratorAccess(key = "#modifyCategory.toKey()")
    public void modifyCustomCategory(ModifyCategory modifyCategory) {
        categoryModifier.modify(modifyCategory);
    }

    /**
     * 삭제 대상 카테고리에 속한 모든 음식을 '기타' 카테고리로 이동한 후 대상 카테고리를 삭제
     */
    @ValidateRefrigeratorAccess(key = "#deleteCategory.toKey()")
    public void removeCustomCategory(DeleteCategory deleteCategory) {
        categoryRemover.remove(deleteCategory);
    }

}