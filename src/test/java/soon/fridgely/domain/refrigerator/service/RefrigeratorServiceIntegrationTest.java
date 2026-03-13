package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class RefrigeratorServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefrigeratorService refrigeratorService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @MockitoBean
    private RefrigeratorManager refrigeratorManager;

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
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );
    }

    @Test
    void 낙관적_락_충돌_발생_시_최대_3번_재시도한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        InvitationCode succeedCode = fixtureMonkey.giveMeOne(InvitationCode.class);

        given(refrigeratorManager.refreshInvitationCode(anyLong(), anyString(), any()))
            .willThrow(new ObjectOptimisticLockingFailureException(Refrigerator.class, refrigerator.getId()))
            .willThrow(new ObjectOptimisticLockingFailureException(Refrigerator.class, refrigerator.getId()))
            .willReturn(succeedCode);

        // when
        InvitationCodeResponse response = refrigeratorService.generateInvitationCode(key);

        // then
        assertThat(response.code()).isEqualTo(succeedCode.getCode());
        verify(refrigeratorManager, times(3)).refreshInvitationCode(anyLong(), anyString(), any());
    }

    @Test
    void MEMBER가_냉장고를_나가면_연결이_해제된다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER)
                .sample()
        );
        var key = new MemberRefrigeratorKey(guestMember.getId(), refrigerator.getId());

        // when
        refrigeratorService.leaveRefrigerator(key);

        // then
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(refrigerator.getId(), guestMember.getId(), EntityStatus.ACTIVE)).isFalse();
    }

    @Test
    void OWNER가_냉장고를_나가려하면_예외가_발생한다() {
        // given - setUp에서 OWNER로 연결된 member 사용
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // expected
        assertThatThrownBy(() -> refrigeratorService.leaveRefrigerator(key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.OWNER_CANNOT_LEAVE_REFRIGERATOR);
    }

    @Test
    void 재시도_횟수를_모두_소진하면_Fallback_메서드가_실행되어_예외가_발생한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        given(refrigeratorManager.refreshInvitationCode(anyLong(), anyString(), any()))
            .willThrow(new ObjectOptimisticLockingFailureException(Refrigerator.class, refrigerator.getId()));

        // expected
        assertThatThrownBy(() -> refrigeratorService.generateInvitationCode(key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.CONCURRENT_UPDATE_LIMIT_EXCEEDED);

        verify(refrigeratorManager, times(3)).refreshInvitationCode(anyLong(), anyString(), any());
    }

}