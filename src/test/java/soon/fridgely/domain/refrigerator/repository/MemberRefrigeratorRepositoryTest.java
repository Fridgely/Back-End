package soon.fridgely.domain.refrigerator.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class MemberRefrigeratorRepositoryTest extends IntegrationTestSupport {

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
    void 내가_속한_냉장고_목록을_조회한다() {
        // given
        Member other = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        Refrigerator myRefrigerator2 = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        memberRefrigeratorRepository.saveAll(
            List.of(
                memberRefrigerator(fixtureMonkey, refrigerator, member)
                    .set("role", RefrigeratorRole.OWNER)
                    .sample(),
                memberRefrigerator(fixtureMonkey, myRefrigerator2, member)
                    .set("role", RefrigeratorRole.MEMBER)
                    .sample(),
                memberRefrigerator(fixtureMonkey, otherRefrigerator, other) // 다른 사람의 냉장고
                    .sample()
            )
        );

        // when
        List<MemberRefrigerator> result = memberRefrigeratorRepository.findAllMyRefrigerators(member.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(result).hasSize(2)
            .extracting("refrigerator.id", "role")
            .containsExactlyInAnyOrder(
                tuple(refrigerator.getId(), RefrigeratorRole.OWNER),
                tuple(myRefrigerator2.getId(), RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 특정_냉장고에_대한_내_정보를_조회한다() {
        // given
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );

        // when
        MemberRefrigerator result = memberRefrigeratorRepository
            .findByMemberIdAndRefrigeratorId(member.getId(), refrigerator.getId(), EntityStatus.ACTIVE)
            .orElseThrow();

        // then
        assertThat(result)
            .extracting("refrigerator.id", "role")
            .containsExactly(refrigerator.getId(), RefrigeratorRole.OWNER);
    }

}