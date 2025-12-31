package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class RefrigeratorManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefrigeratorManager refrigeratorManager;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    void 냉장고를_생성한다() {
        // given
        Member newMember = memberRepository.save(
            member(fixtureMonkey)
                .set("nickname", "nickname")
                .sample()
        );

        // when
        Refrigerator savedRefrigerator = refrigeratorManager.register(newMember);

        // then
        assertThat(savedRefrigerator)
            .extracting("name")
            .isEqualTo("nickname의 냉장고");
    }

    @Test
    void 냉장고_이름을_수정한다() {
        // given
        String newName = "새로운 이름";

        // when
        refrigeratorManager.update(refrigerator.getId(), newName);

        // then
        Refrigerator updated = refrigeratorRepository.findById(refrigerator.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo(newName);
    }

    @Test
    void 냉장고의_초대_코드를_발급하고_저장한다() {
        // given
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
        String code = "VALID123";
        refrigeratorManager.refreshInvitationCode(refrigerator.getId(), code, LocalDateTime.now());

        // when
        Refrigerator found = refrigeratorManager.findByInvitationCode(code);

        // then
        assertThat(found)
            .extracting("id", "name")
            .containsExactly(refrigerator.getId(), refrigerator.getName());
    }

    @Test
    void 존재하지_않는_초대_코드로_조회하면_예외가_발생한다() {
        // given
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

}