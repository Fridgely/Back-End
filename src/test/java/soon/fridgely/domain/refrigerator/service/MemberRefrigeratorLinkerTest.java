package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRefrigeratorLinkerTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 회원과_냉장고를_연결하며_권한은_OWNER로_설정된다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        // when
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // then
        MemberRefrigerator memberRefrigerator = memberRefrigeratorRepository.findByMemberAndRefrigerator(member, refrigerator).orElseThrow();
        assertThat(memberRefrigerator)
            .extracting("member", "refrigerator", "role")
            .containsExactly(member, refrigerator, RefrigeratorRole.OWNER);
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