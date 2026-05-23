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
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.RedisIntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@TestPropertySource(properties = "spring.cache.type=redis")
class RefrigeratorCacheIntegrationTest extends RedisIntegrationTestSupport {

    @Autowired
    private RefrigeratorService refrigeratorService;

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
        this.member = memberRepository.save(member().sample());
        this.refrigerator = refrigeratorRepository.save(refrigerator().sample());

        Cache cache = cacheManager.getCache("myRefrigerators");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void 냉장고_목록_조회_시_캐시가_적용된다() {
        // given
        refrigeratorService.linkToOwner(member, refrigerator);

        // when
        List<RefrigeratorResponse> firstResult = refrigeratorService.findAllMyRefrigerators(member.getId());

        // then
        assertThat(firstResult).hasSize(1);
        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache).isNotNull();
        assertThat(cache.get(member.getId())).isNotNull();

        List<RefrigeratorResponse> secondResult = refrigeratorService.findAllMyRefrigerators(member.getId());
        assertThat(secondResult).hasSize(1);
        assertThat(firstResult).isEqualTo(secondResult);
    }

    @Test
    void 냉장고_생성시_캐시가_무효화된다() {
        // given
        refrigeratorService.linkToOwner(member, refrigerator);
        List<RefrigeratorResponse> cachedResult = refrigeratorService.findAllMyRefrigerators(member.getId());
        assertThat(cachedResult).hasSize(1);

        // when
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator().sample());
        refrigeratorService.linkToOwner(member, refrigerator2);

        // then
        List<RefrigeratorResponse> updatedResult = refrigeratorService.findAllMyRefrigerators(member.getId());
        assertThat(updatedResult).hasSize(2)
            .extracting(RefrigeratorResponse::id)
            .containsExactlyInAnyOrder(refrigerator.getId(), refrigerator2.getId());
    }

    @Test
    void 초대_코드로_냉장고_참여시_해당_멤버의_캐시만_무효화된다() {
        // given
        Member member2 = memberRepository.save(member().sample());
        refrigeratorService.linkToOwner(member, refrigerator);

        var ownerKey = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        var invCode = refrigeratorService.generateInvitationCode(ownerKey);

        refrigeratorService.findAllMyRefrigerators(member.getId());
        refrigeratorService.findAllMyRefrigerators(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        refrigeratorService.joinByInvitationCode(member2.getId(), invCode.code());

        // then
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNull();

        List<RefrigeratorResponse> member2Refrigerators = refrigeratorService.findAllMyRefrigerators(member2.getId());
        assertThat(member2Refrigerators).hasSize(1)
            .extracting(RefrigeratorResponse::id)
            .containsExactly(refrigerator.getId());
    }

    @Test
    void 냉장고_이름_수정시_모든_멤버의_캐시가_무효화된다() {
        // given
        Member member2 = memberRepository.save(member().sample());
        refrigeratorService.linkToOwner(member, refrigerator);

        var ownerKey = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        var invCode = refrigeratorService.generateInvitationCode(ownerKey);
        refrigeratorService.joinByInvitationCode(member2.getId(), invCode.code());

        refrigeratorService.findAllMyRefrigerators(member.getId());
        refrigeratorService.findAllMyRefrigerators(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        String newName = "새로운 냉장고 이름";
        refrigeratorService.updateRefrigeratorName(ownerKey,
            new soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest(newName));

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNull();

        List<RefrigeratorResponse> member1Refrigerators = refrigeratorService.findAllMyRefrigerators(member.getId());
        List<RefrigeratorResponse> member2Refrigerators = refrigeratorService.findAllMyRefrigerators(member2.getId());

        assertThat(member1Refrigerators)
            .extracting(RefrigeratorResponse::name)
            .containsExactly(newName);
        assertThat(member2Refrigerators)
            .extracting(RefrigeratorResponse::name)
            .containsExactly(newName);
    }

    @Test
    void 서로_다른_멤버의_냉장고_목록_캐시는_독립적으로_동작한다() {
        // given
        Member member2 = memberRepository.save(member().sample());
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator().sample());

        refrigeratorService.linkToOwner(member, refrigerator);
        refrigeratorService.linkToOwner(member2, refrigerator2);

        refrigeratorService.findAllMyRefrigerators(member.getId());
        refrigeratorService.findAllMyRefrigerators(member2.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        // when
        Refrigerator refrigerator3 = refrigeratorRepository.save(refrigerator().sample());
        refrigeratorService.linkToOwner(member, refrigerator3);

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNotNull();

        List<RefrigeratorResponse> memberRefrigerators = refrigeratorService.findAllMyRefrigerators(member.getId());
        assertThat(memberRefrigerators).hasSize(2)
            .extracting(RefrigeratorResponse::id)
            .containsExactlyInAnyOrder(refrigerator.getId(), refrigerator3.getId());
    }

    @Test
    void 캐시_키가_memberId로_정확하게_생성된다() {
        // given
        refrigeratorService.linkToOwner(member, refrigerator);

        // when
        refrigeratorService.findAllMyRefrigerators(member.getId());

        // then
        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache).isNotNull();
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member.getId()).get()).isInstanceOf(List.class);
    }

    @Test
    void allEntries_true로_전체_캐시가_무효화된다() {
        // given
        Member member2 = memberRepository.save(member().sample());
        Member member3 = memberRepository.save(member().sample());
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator().sample());

        refrigeratorService.linkToOwner(member, refrigerator);
        refrigeratorService.linkToOwner(member2, refrigerator2);
        refrigeratorService.linkToOwner(member3, refrigerator2);

        refrigeratorService.findAllMyRefrigerators(member.getId());
        refrigeratorService.findAllMyRefrigerators(member2.getId());
        refrigeratorService.findAllMyRefrigerators(member3.getId());

        Cache cache = cacheManager.getCache("myRefrigerators");
        assertThat(cache.get(member.getId())).isNotNull();
        assertThat(cache.get(member2.getId())).isNotNull();
        assertThat(cache.get(member3.getId())).isNotNull();

        // when: member2가 소유한 refrigerator2의 이름 수정 (allEntries=true evict 발생)
        var member2Key = new MemberRefrigeratorKey(member2.getId(), refrigerator2.getId());
        refrigeratorService.updateRefrigeratorName(member2Key,
            new soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest("업데이트된 냉장고"));

        // then
        assertThat(cache.get(member.getId())).isNull();
        assertThat(cache.get(member2.getId())).isNull();
        assertThat(cache.get(member3.getId())).isNull();
    }

    @Test
    void 캐시된_데이터와_DB_데이터가_일치한다() {
        // given
        refrigeratorService.linkToOwner(member, refrigerator);

        // when
        List<RefrigeratorResponse> cachedResult = refrigeratorService.findAllMyRefrigerators(member.getId());
        List<RefrigeratorResponse> freshResult = memberRefrigeratorRepository
            .findAllMyRefrigerators(member.getId(), soon.fridgely.domain.EntityStatus.ACTIVE)
            .stream()
            .map(RefrigeratorResponse::from)
            .toList();

        // then
        assertThat(cachedResult).hasSize(freshResult.size());
        assertThat(cachedResult)
            .extracting(RefrigeratorResponse::id)
            .containsExactlyInAnyOrderElementsOf(
                freshResult.stream().map(RefrigeratorResponse::id).toList()
            );
    }
}
