package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static soon.fridgely.domain.category.entity.Category.register;

@RequiredArgsConstructor
@Component
public class CategoryAppender {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
        "야채", "과일", "육류", "해산물", "유제품", "음료", "간식", "기타"
    );

    private final CategoryRepository categoryRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final MemberRepository memberRepository;

    public void appendDefaultCategories(MemberRefrigeratorKey key) {
        CategoryContext context = getContext(key);
        if (categoryRepository.existsByRefrigeratorAndStatus(context.refrigerator(), EntityStatus.ACTIVE)) {
            return;
        }

        List<Category> categories = DEFAULT_CATEGORIES.stream()
            .map(categoryName -> register(categoryName, context.refrigerator(), context.member(), CategoryType.DEFAULT))
            .toList();
        categoryRepository.saveAll(categories);
    }

    @Transactional
    public void appendCustomCategory(AddCategory addCategory) {
        CategoryContext context = getContext(addCategory.toKey());
        Category category = register(addCategory.name(), context.refrigerator(), context.member(), CategoryType.CUSTOM);
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.DUPLICATE_CATEGORY_NAME, addCategory.name());
        }
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