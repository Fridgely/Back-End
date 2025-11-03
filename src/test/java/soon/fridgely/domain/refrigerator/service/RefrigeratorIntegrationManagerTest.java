package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

import static org.assertj.core.api.Assertions.assertThat;

class RefrigeratorIntegrationManagerTest extends IntegrationTestSupport {

    @Autowired
    private RefrigeratorManager refrigeratorManager;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 냉장고를_생성한다() {
        // given
        Member member = createMember();

        // when
        Refrigerator refrigerator = refrigeratorManager.register(member);

        // then
        Refrigerator savedRefrigerator = refrigeratorRepository.findById(refrigerator.getId()).orElseThrow();
        assertThat(savedRefrigerator)
            .extracting("name")
            .isEqualTo(member.getNickname() + "의 냉장고");
    }

    private Member createMember() {
        return Member.builder()
            .nickname("testNickname")
            .build();
    }

}