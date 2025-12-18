package soon.fridgely.domain.refrigerator.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MemberRefrigeratorRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 내가_속한_냉장고_목록을_조회한다() {
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
        List<MemberRefrigerator> result = memberRefrigeratorRepository.findAllMyRefrigerators(me.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(result).hasSize(2)
            .extracting("refrigerator.name", "role")
            .containsExactly(
                tuple("me의 냉장고", RefrigeratorRole.OWNER),
                tuple("me의 냉장고", RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 특정_냉장고에_대한_내_정보를_조회한다() {
        // given
        Member member = createMember("me");
        memberRepository.save(member);

        Refrigerator fridge = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(fridge);

        memberRefrigeratorRepository.save(MemberRefrigerator.link(member, fridge, RefrigeratorRole.OWNER));

        // when
        MemberRefrigerator result = memberRefrigeratorRepository
            .findByMemberIdAndRefrigeratorId(member.getId(), fridge.getId(), EntityStatus.ACTIVE)
            .orElseThrow();

        // then
        assertThat(result.getRefrigerator().getId()).isEqualTo(fridge.getId());
        assertThat(result.getRole()).isEqualTo(RefrigeratorRole.OWNER);
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