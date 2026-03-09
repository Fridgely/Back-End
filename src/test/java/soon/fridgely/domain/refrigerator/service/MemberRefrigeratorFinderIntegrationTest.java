package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.CachedMemberRefrigerators;
import soon.fridgely.domain.refrigerator.dto.command.CachedRefrigeratorInfo;
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
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class MemberRefrigeratorFinderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorFinder memberRefrigeratorFinder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

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
    void 내가_속한_모든_냉장고_목록을_조회한다() {
        // given
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        Refrigerator myRefrigerator2 = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        Refrigerator otherFridge = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        memberRefrigeratorRepository.saveAll(List.of(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample(),
            memberRefrigerator(fixtureMonkey, myRefrigerator2, member)
                .set("role", RefrigeratorRole.MEMBER)
                .sample(),
            memberRefrigerator(fixtureMonkey, otherFridge, otherMember) // 다른 사람의 냉장고
                .sample()
        ));

        // when
        CachedMemberRefrigerators result = memberRefrigeratorFinder.findAllByMemberId(member.getId());

        // then
        assertThat(result.refrigerators()).hasSize(2)
            .extracting(CachedRefrigeratorInfo::id, CachedRefrigeratorInfo::role)
            .containsExactlyInAnyOrder(
                tuple(refrigerator.getId(), RefrigeratorRole.OWNER),
                tuple(myRefrigerator2.getId(), RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 특정_냉장고에_대한_멤버십_정보를_조회한다() {
        // given
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );

        // when
        MemberRefrigerator result = memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(member.getId(), refrigerator.getId());

        // then
        assertThat(result).isNotNull()
            .extracting("refrigerator.id", "role")
            .containsExactly(refrigerator.getId(), RefrigeratorRole.OWNER);
    }

    @Test
    void 멤버십_정보가_존재하지_않으면_예외가_발생한다() {
        // expected
        assertThatThrownBy(() -> memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(member.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 냉장고와_멤버의_연결을_확인한다() {
        // given
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.MEMBER)
                .sample()
        );

        // when
        boolean exists = memberRefrigeratorFinder.existsByRefrigeratorIdAndMemberId(refrigerator.getId(), member.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void 냉장고와_멤버가_연결되지_않은_경우_false를_반환한다() {
        // when
        boolean exists = memberRefrigeratorFinder.existsByRefrigeratorIdAndMemberId(refrigerator.getId(), member.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void 냉장고에_속한_팀원_목록을_조회한다() {
        // given
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        memberRefrigeratorRepository.saveAll(List.of(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample(),
            memberRefrigerator(fixtureMonkey, refrigerator, otherMember)
                .set("role", RefrigeratorRole.MEMBER)
                .sample(),
            memberRefrigerator(fixtureMonkey, otherRefrigerator, otherMember) // 다른 냉장고
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        ));

        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllMembersByRefrigeratorId(refrigerator.getId());

        // then
        assertThat(result).hasSize(2)
            .extracting("member.id", "role")
            .containsExactlyInAnyOrder(
                tuple(member.getId(), RefrigeratorRole.OWNER),
                tuple(otherMember.getId(), RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 팀원이_없는_냉장고는_빈_목록을_반환한다() {
        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllMembersByRefrigeratorId(refrigerator.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void MemberRefrigerator가_삭제_상태이면_팀원_목록_조회에서_제외된다() {
        // given
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .set("status", EntityStatus.DELETED)
                .sample()
        );

        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllMembersByRefrigeratorId(refrigerator.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void Member가_삭제_상태이면_팀원_목록_조회에서_제외된다() {
        // given
        Member deletedMember = memberRepository.save(
            member(fixtureMonkey)
                .set("status", EntityStatus.DELETED)
                .sample()
        );
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, deletedMember)
                .set("role", RefrigeratorRole.MEMBER)
                .sample()
        );

        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllMembersByRefrigeratorId(refrigerator.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void Refrigerator가_삭제_상태이면_팀원_목록_조회에서_제외된다() {
        // given
        Refrigerator deletedRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey)
                .set("status", EntityStatus.DELETED)
                .sample()
        );
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, deletedRefrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );

        // when
        List<MemberRefrigerator> result = memberRefrigeratorFinder.findAllMembersByRefrigeratorId(deletedRefrigerator.getId());

        // then
        assertThat(result).isEmpty();
    }

}