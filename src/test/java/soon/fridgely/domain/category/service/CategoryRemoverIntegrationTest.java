package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRemoverIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryRemover categoryRemover;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void 일반_카테고리를_삭제한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category category = Category.register("카테고리", refrigerator, member, CategoryType.CUSTOM);
        categoryRepository.save(category);

        var deleteCategory = new DeleteCategory(member.getId(), refrigerator.getId(), category.getId());

        // when
        categoryRemover.remove(deleteCategory);

        // then
        Category deletedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}