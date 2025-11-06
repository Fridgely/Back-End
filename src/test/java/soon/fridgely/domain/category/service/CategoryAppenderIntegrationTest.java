package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryAppenderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryAppender categoryAppender;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void 냉장고ID와_멤버ID를_받아_기본_카테고리들을_일괄_생성한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        // when
        categoryAppender.appendDefaultCategories(refrigerator.getId(), member.getId());

        // then
        List<Category> categories = categoryRepository.findAllByRefrigeratorAndStatus(refrigerator, EntityStatus.ACTIVE);

        assertThat(categories)
            .hasSize(8)
            .extracting(Category::getName)
            .containsExactlyInAnyOrder("야채", "과일", "육류", "해산물", "유제품", "음료", "간식", "기타");
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