package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@TestPropertySource(properties = "spring.cache.type=caffeine")
class RefrigeratorCacheIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRefrigeratorFinder memberRefrigeratorFinder;

    @Autowired
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Autowired
    private RefrigeratorManager refrigeratorManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private CacheManager cacheManager;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(member(fixtureMonkey).sample());
        this.refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        Cache cache = cacheManager.getCache("myRefrigerators");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void 냉장고_목록_조회_시_캐시가_적용된다() {
        // given
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // when
        List<MemberRefrigerator> firstResult = memberRefrigeratorFinder.findAllByMemberId(member.getId());

        // then
        assertThat(firstResult).hasSize(1);
        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache).isNotNull();
        assertThat(cache.get(member.getId())).isNotNull();

        List<MemberRefrigerator> secondResult = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        assertThat(secondResult).hasSize(1);
        assertThat(firstResult).isEqualTo(secondResult);
    }

    @Test
    void 냉장고_생성시_캐시가_무효화된다() {
        // given
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);
        List<MemberRefrigerator> cachedResult = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        assertThat(cachedResult).hasSize(1);

        // when
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorLinker.linkToOwner(member, refrigerator2);

        // then
        List<MemberRefrigerator> updatedResult = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        assertThat(updatedResult).hasSize(2)
            .extracting(mr -> mr.getRefrigerator().getId())
            .containsExactlyInAnyOrder(refrigerator.getId(), refrigerator2.getId());
    }

    @Test
    void 초대_코드로_냉장고_참여시_해당_멤버의_캐시만_무효화된다() {
        // given
        Member member2 = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        memberRefrigeratorFinder.findAllByMemberId(member.getId());
        memberRefrigeratorFinder.findAllByMemberId(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member2.getId(), refrigerator.getId());
        memberRefrigeratorLinker.linkMemberToRefrigerator(key, RefrigeratorRole.MEMBER);

        // then
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNull();

        List<MemberRefrigerator> member2Refrigerators = memberRefrigeratorFinder.findAllByMemberId(member2.getId());
        assertThat(member2Refrigerators).hasSize(1)
            .extracting(mr -> mr.getRefrigerator().getId())
            .containsExactly(refrigerator.getId());
    }

    @Test
    void 냉장고_이름_수정시_모든_멤버의_캐시가_무효화된다() {
        // given
        Member member2 = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member2.getId(), refrigerator.getId());
        memberRefrigeratorLinker.linkMemberToRefrigerator(key, RefrigeratorRole.MEMBER);

        memberRefrigeratorFinder.findAllByMemberId(member.getId());
        memberRefrigeratorFinder.findAllByMemberId(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        String newName = "새로운 냉장고 이름";
        refrigeratorManager.update(refrigerator.getId(), newName);

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNull();

        List<MemberRefrigerator> member1Refrigerators = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        List<MemberRefrigerator> member2Refrigerators = memberRefrigeratorFinder.findAllByMemberId(member2.getId());

        assertThat(member1Refrigerators.get(0).getRefrigerator().getName()).isEqualTo(newName);
        assertThat(member2Refrigerators.get(0).getRefrigerator().getName()).isEqualTo(newName);
    }

    @Test
    void 서로_다른_멤버의_냉장고_목록_캐시는_독립적으로_동작한다() {
        // given
        Member member2 = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        memberRefrigeratorLinker.linkToOwner(member, refrigerator);
        memberRefrigeratorLinker.linkToOwner(member2, refrigerator2);

        memberRefrigeratorFinder.findAllByMemberId(member.getId());
        memberRefrigeratorFinder.findAllByMemberId(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        Refrigerator refrigerator3 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorLinker.linkToOwner(member, refrigerator3);

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        List<MemberRefrigerator> memberRefrigerators = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        assertThat(memberRefrigerators).hasSize(2)
            .extracting(mr -> mr.getRefrigerator().getId())
            .containsExactlyInAnyOrder(refrigerator.getId(), refrigerator3.getId());
    }

    @Test
    void 캐시_키가_memberId로_정확하게_생성된다() {
        // given
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // when
        memberRefrigeratorFinder.findAllByMemberId(member.getId());

        // then
        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache).isNotNull();
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member.getId()).get()).isInstanceOf(List.class);
    }

    @Test
    void allEntries_true로_전체_캐시가_무효화된다() {
        // given
        Member member2 = memberRepository.save(member(fixtureMonkey).sample());
        Member member3 = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        memberRefrigeratorLinker.linkToOwner(member, refrigerator);
        memberRefrigeratorLinker.linkToOwner(member2, refrigerator2);
        memberRefrigeratorLinker.linkToOwner(member3, refrigerator2);

        memberRefrigeratorFinder.findAllByMemberId(member.getId());
        memberRefrigeratorFinder.findAllByMemberId(member2.getId());
        memberRefrigeratorFinder.findAllByMemberId(member3.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();
        assertThat(cache.get(member3.getId())).isNotNull();

        // when
        refrigeratorManager.update(refrigerator2.getId(), "업데이트된 냉장고");

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNull();
        assertThat(cache.get(member3.getId())).isNull();
    }

    @Test
    void 캐시된_데이터와_DB_데이터가_일치한다() {
        // given
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // when
        List<MemberRefrigerator> cachedResult = memberRefrigeratorFinder.findAllByMemberId(member.getId());
        List<MemberRefrigerator> dbResult = memberRefrigeratorRepository.findAllMyRefrigerators(
            member.getId(),
            soon.fridgely.domain.EntityStatus.ACTIVE
        );

        // then
        assertThat(cachedResult).hasSize(dbResult.size());
        assertThat(cachedResult)
            .extracting(mr -> mr.getRefrigerator().getId())
            .containsExactlyInAnyOrderElementsOf(
                dbResult.stream().map(mr -> mr.getRefrigerator().getId()).toList()
            );
    }

}