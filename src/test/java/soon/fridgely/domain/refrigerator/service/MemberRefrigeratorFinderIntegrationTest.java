package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MemberRefrigeratorFinderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorFinder memberRefrigeratorFinder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Test
    void 내가_속한_모든_냉장고_목록을_조회한다() {
        // given
        Member me = createMember("me");
        Member other = createMember("other");
        memberRepository.saveAll(List.of(me, other));

        Refrigerator myFridge1 = Refrigerator.register(me.getNickname());
        Refrigerator myFridge2 = Refrigerator.register(me.getNickname());
        Refrigerator otherFridge = Refrigerator.register(other.getNickname());
        refrigeratorRepository.saveAll(List.of(myFridge1, myFridge2, otherFridge));

        memberRefrigeratorRepository.save(MemberRefrigerator.link(me, myFridge1, RefrigeratorRole.OWNER));
        memberRefrigeratorRepository.save(MemberRefrigerator.link(me, myFridge2, RefrigeratorRole.MEMBER));
        memberRefrigeratorRepository.save(MemberRefrigerator.link(other, otherFridge, RefrigeratorRole.OWNER));

        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllByMemberId(me.getId());

        // then
        assertThat(result).hasSize(2)
            .extracting("refrigerator.name", "role")
            .containsExactly(
                tuple("me의 냉장고", RefrigeratorRole.OWNER),
                tuple("me의 냉장고", RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 특정_냉장고에_대한_멤버십_정보를_조회한다() {
        // given
        Member member = createMember("test");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        memberRefrigeratorRepository.save(MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER));

        // when
        MemberRefrigerator result = memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(member.getId(), refrigerator.getId());

        // then
        assertThat(result).isNotNull()
            .extracting("refrigerator.name", "role")
            .containsExactly(refrigerator.getName(), RefrigeratorRole.OWNER);
    }

    @Test
    void 멤버십_정보가_존재하지_않으면_예외가_발생한다() {
        // given
        Member member = createMember("test");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register("Test Fridge");
        refrigeratorRepository.save(refrigerator);

        // expected
        assertThatThrownBy(() -> memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(member.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 냉장고와_멤버의_연결을_확인한다() {
        // given
        Member member = createMember("test");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        memberRefrigeratorRepository.save(MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER));

        // when
        boolean exists = memberRefrigeratorFinder.existsByRefrigeratorIdAndMemberId(refrigerator.getId(), member.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void 냉장고와_멤버가_연결되지_않은_경우_false를_반환한다() {
        // given

        Member member = createMember("test");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        // when
        boolean exists = memberRefrigeratorFinder.existsByRefrigeratorIdAndMemberId(refrigerator.getId(), member.getId());

        // then
        assertThat(exists).isFalse();
    }

    private Member createMember(String nickname) {
        return Member.builder()
            .loginId(nickname)
            .password("pw")
            .nickname(nickname)
            .role(MemberRole.MEMBER)
            .build();
    }

}