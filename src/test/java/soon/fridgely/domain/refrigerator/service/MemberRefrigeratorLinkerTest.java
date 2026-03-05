package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class MemberRefrigeratorLinkerTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
    }

    @Test
    void 회원과_냉장고를_연결하며_권한은_OWNER로_설정된다() {
        // when
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // then
        MemberRefrigerator memberRefrigerator = memberRefrigeratorRepository.findByMemberAndRefrigerator(member, refrigerator).orElseThrow();
        assertThat(memberRefrigerator)
            .extracting("member.id", "refrigerator.id", "role")
            .containsExactly(member.getId(), refrigerator.getId(), RefrigeratorRole.OWNER);
    }

    @Test
    void 회원과_냉장고를_연결하며_지정한_권한으로_설정된다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        memberRefrigeratorLinker.linkMemberToRefrigerator(key, RefrigeratorRole.MEMBER);

        // then
        MemberRefrigerator memberRefrigerator = memberRefrigeratorRepository.findByMemberAndRefrigerator(member, refrigerator).orElseThrow();
        assertThat(memberRefrigerator)
            .extracting("member.id", "refrigerator.id", "role")
            .containsExactly(member.getId(), refrigerator.getId(), RefrigeratorRole.MEMBER);
    }

    @Test
    void 이미_가입한_냉장고에_다시_가입하려하면_예외가_발생한다() {
        // given
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member).sample()
        );

        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // expected
        assertThatThrownBy(() -> memberRefrigeratorLinker.linkMemberToRefrigerator(key, RefrigeratorRole.MEMBER))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ALREADY_JOINED_REFRIGERATOR);
    }

}