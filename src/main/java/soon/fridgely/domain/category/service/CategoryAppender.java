package soon.fridgely.domain.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
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

    public void appendDefaultCategories(long refrigeratorId, long memberId) {
        Refrigerator refrigerator = refrigeratorRepository.findById(refrigeratorId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));

        List<Category> categories = DEFAULT_CATEGORIES.stream()
            .map(categoryName -> register(categoryName, refrigerator, member))
            .toList();
        categoryRepository.saveAll(categories);
    }

}