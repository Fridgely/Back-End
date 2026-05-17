package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.CachedCategories;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.dto.response.CategoryDetailResponse;
import soon.fridgely.domain.category.dto.response.CategoryResponse;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.category.validator.CategoryValidator;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static soon.fridgely.domain.category.entity.Category.register;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
        "야채", "과일", "육류", "해산물", "유제품", "음료", "간식", "기타"
    );
    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final CategoryRepository categoryRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final MemberRepository memberRepository;
    private final FoodRepository foodRepository;
    private final CategoryValidator categoryValidator;

    @CacheEvict(value = "categories", key = "#key.refrigeratorId()")
    @Transactional
    public void appendDefaultCategories(MemberRefrigeratorKey key) {
        CategoryContext context = getContext(key);
        if (categoryRepository.existsByRefrigeratorAndStatus(context.refrigerator(), EntityStatus.ACTIVE)) {
            return;
        }
        List<Category> categories = DEFAULT_CATEGORIES.stream()
            .map(name -> register(name, context.refrigerator(), context.member(), CategoryType.DEFAULT))
            .toList();
        categoryRepository.saveAll(categories);
    }

    @ValidateRefrigeratorAccess(key = "#addCategory.toKey()")
    @CacheEvict(value = "categories", key = "#addCategory.refrigeratorId()")
    @Transactional
    public void appendCustomCategory(AddCategory addCategory) {
        CategoryContext context = getContext(addCategory.toKey());
        Category category = register(addCategory.name(), context.refrigerator(), context.member(), CategoryType.CUSTOM);
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new CoreException(ErrorType.DUPLICATE_CATEGORY_NAME, addCategory.name());
            }
            throw e;
        }
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public CategoryDetailResponse findCategory(long categoryId, MemberRefrigeratorKey key) {
        return CategoryDetailResponse.from(findByRefrigerator(categoryId, key.refrigeratorId()));
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCategory(MemberRefrigeratorKey key) {
        List<Category> categories = categoryRepository.findAllByRefrigeratorIdAndStatus(
            key.refrigeratorId(), EntityStatus.ACTIVE
        );
        return CachedCategories.from(key.refrigeratorId(), categories)
            .categories()
            .stream()
            .map(info -> new CategoryResponse(info.id(), info.name(), info.isDefaultType()))
            .toList();
    }

    @ValidateRefrigeratorAccess(key = "#modifyCategory.toKey()")
    @CacheEvict(value = "categories", key = "#modifyCategory.refrigeratorId()")
    @Transactional
    public void modifyCustomCategory(ModifyCategory modifyCategory) {
        Category category = findByRefrigerator(modifyCategory.categoryId(), modifyCategory.refrigeratorId());
        if (category.isSameName(modifyCategory.newName())) {
            return;
        }
        categoryValidator.validateNotDefaultType(category);
        categoryValidator.validateDuplicateName(modifyCategory.newName(), category.getRefrigerator());
        category.updateName(modifyCategory.newName());
    }

    @ValidateRefrigeratorAccess(key = "#deleteCategory.toKey()")
    @CacheEvict(value = "categories", key = "#deleteCategory.refrigeratorId()")
    @Transactional
    public void removeCustomCategory(DeleteCategory deleteCategory) {
        Category category = categoryRepository.findByIdAndRefrigeratorId(
                deleteCategory.categoryId(), deleteCategory.refrigeratorId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        if (category.isDeleted()) {
            return;
        }
        categoryValidator.validateNotDefaultType(category);

        Category fallbackCategory = categoryRepository.findByNameAndRefrigeratorIdAndStatus(
                FALLBACK_CATEGORY_NAME, deleteCategory.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        foodRepository.moveAllFoodsToFallbackCategory(category, fallbackCategory);

        category.delete();
        categoryRepository.save(category); // clearAutomatically=true로 영속성 컨텍스트가 초기화되므로 명시적 저장 필요
    }

    @CacheEvict(value = "categories", key = "#refrigeratorId")
    @Transactional
    public void removeAllByRefrigeratorId(long refrigeratorId) {
        List<Category> categories = categoryRepository.findAllByRefrigeratorIdAndStatus(refrigeratorId, EntityStatus.ACTIVE);
        categories.forEach(Category::delete);
    }

    @Cacheable(value = "categories", key = "#refrigeratorId")
    @Transactional(readOnly = true)
    public CachedCategories findAll(long refrigeratorId) {
        List<Category> categories = categoryRepository.findAllByRefrigeratorIdAndStatus(refrigeratorId, EntityStatus.ACTIVE);
        return CachedCategories.from(refrigeratorId, categories);
    }

    @Transactional(readOnly = true)
    public Category findByRefrigerator(long categoryId, long refrigeratorId) {
        return categoryRepository.findByIdAndRefrigeratorIdAndStatus(categoryId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public Category findByName(String categoryName, long refrigeratorId) {
        return categoryRepository.findByNameAndRefrigeratorIdAndStatus(categoryName, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    private CategoryContext getContext(MemberRefrigeratorKey key) {
        Member member = memberRepository.findByIdAndStatus(key.memberId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        return new CategoryContext(refrigerator, member);
    }

    private record CategoryContext(Refrigerator refrigerator, Member member) {
    }
}
