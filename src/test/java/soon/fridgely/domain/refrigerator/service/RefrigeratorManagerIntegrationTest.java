package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefrigeratorManagerIntegrationTest extends IntegrationTestSupport {

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

    @Test
    void 냉장고_이름을_수정한다() {
        // given
        Refrigerator refrigerator = Refrigerator.register("예전 냉장고");
        refrigeratorRepository.save(refrigerator);

        // when
        refrigeratorManager.update(refrigerator.getId(), "새로운 냉장고");

        // then
        Refrigerator updated = refrigeratorRepository.findById(refrigerator.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("새로운 냉장고");
    }

    @Test
    void 냉장고의_초대_코드를_발급하고_저장한다() {
        // given
        Refrigerator refrigerator = Refrigerator.register("냉장고");
        refrigeratorRepository.save(refrigerator);

        String newCode = "NEWCODE1";
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        // when
        InvitationCode invitationCode = refrigeratorManager.refreshInvitationCode(refrigerator.getId(), newCode, now);

        // then
        assertThat(invitationCode).isNotNull()
            .extracting("code", "expirationAt")
            .containsExactly(newCode, now.plusDays(1L));
    }

    @Test
    void 유효한_초대_코드로_냉장고를_조회한다() {
        // given
        Refrigerator refrigerator = Refrigerator.register("냉장고");
        refrigeratorRepository.save(refrigerator);

        String code = "VALID123";
        LocalDateTime now = LocalDateTime.now();

        refrigeratorManager.refreshInvitationCode(refrigerator.getId(), code, now);

        // when
        Refrigerator found = refrigeratorManager.findByInvitationCode(code);

        // then
        assertThat(found)
            .extracting("id", "name")
            .containsExactly(refrigerator.getId(), "냉장고의 냉장고");
    }

    @Test
    void 존재하지_않는_초대_코드로_조회하면_예외가_발생한다() {
        // given
        Refrigerator refrigerator = Refrigerator.register("테스트 냉장고");
        refrigeratorRepository.save(refrigerator);

        refrigeratorManager.refreshInvitationCode(refrigerator.getId(), "REALCODE", LocalDateTime.now());

        // expected
        assertThatThrownBy(() -> refrigeratorManager.findByInvitationCode("FAKECODE"))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_INVITATION_CODE);
    }

    @Test
    void 삭제된_냉장고는_유효한_초대_코드로도_조회되지_않는다() {
        // given
        Refrigerator refrigerator = Refrigerator.register("삭제될 냉장고");
        refrigeratorRepository.save(refrigerator);

        String code = "DELETED1";
        refrigeratorManager.refreshInvitationCode(refrigerator.getId(), code, LocalDateTime.now());

        Refrigerator refreshedRefrigerator = refrigeratorRepository.findById(refrigerator.getId()).orElseThrow();
        refreshedRefrigerator.delete();
        refrigeratorRepository.save(refreshedRefrigerator);

        // expected
        assertThatThrownBy(() -> refrigeratorManager.findByInvitationCode(code))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_INVITATION_CODE);
    }

    private Member createMember() {
        return Member.builder()
            .nickname("testNickname")
            .build();
    }

}