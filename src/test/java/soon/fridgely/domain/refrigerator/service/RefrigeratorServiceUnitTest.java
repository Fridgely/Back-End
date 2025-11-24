package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefrigeratorServiceUnitTest {

    @InjectMocks
    private RefrigeratorService refrigeratorService;

    @Mock
    private RefrigeratorManager refrigeratorManager;

    @Mock
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Mock
    private MemberRefrigeratorFinder memberRefrigeratorFinder;

    @Mock
    private InvitationCodeGenerator codeGenerator;

    @Test
    void 냉장고_정보를_수정한다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(1L, 1L);
        RefrigeratorUpdateRequest request = new RefrigeratorUpdateRequest("New Name");

        // when
        refrigeratorService.updateRefrigeratorName(key, request);

        // then
        then(refrigeratorManager).should()
            .update(eq(1L), eq("New Name"));
    }

    @Test
    void 초대_코드를_생성하고_반환한다() {
        // given
        long refrigeratorId = 1L;
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(1L, refrigeratorId);

        String generatedCode = "ABC12345";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime realExpirationAt = now.plusDays(1);

        given(codeGenerator.generate()).willReturn(generatedCode);
        InvitationCode savedCode = new InvitationCode(generatedCode, realExpirationAt);
        given(refrigeratorManager.refreshInvitationCode(eq(refrigeratorId), eq(generatedCode), any(LocalDateTime.class)))
            .willReturn(savedCode);

        // when
        InvitationCodeResponse response = refrigeratorService.generateInvitationCode(key);

        // then
        assertThat(response.code()).isEqualTo(generatedCode);
        assertThat(response.expirationAt()).isEqualTo(realExpirationAt);

        then(refrigeratorManager).should()
            .refreshInvitationCode(
                eq(refrigeratorId),
                eq(generatedCode),
                any(LocalDateTime.class)
            );
    }

    @Test
    void 초대_코드를_사용해_냉장고에_참여한다() {
        // given
        long memberId = 100L;
        String code = "VALIDCODE";
        long refrigeratorId = 50L;

        Refrigerator mockRefrigerator = mock(Refrigerator.class);
        given(refrigeratorManager.findByInvitationCode(code)).willReturn(mockRefrigerator);
        given(mockRefrigerator.getId()).willReturn(refrigeratorId);

        // when
        refrigeratorService.joinByInvitationCode(memberId, code);

        // then
        then(mockRefrigerator).should()
            .validateInvitationCode(eq(code), any(LocalDateTime.class));

        MemberRefrigeratorKey expectedKey = new MemberRefrigeratorKey(memberId, refrigeratorId);
        then(memberRefrigeratorLinker).should()
            .linkMemberToRefrigerator(
                eq(expectedKey),
                eq(RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 내가_속한_냉장고_목록을_조회한다() {
        // given
        long memberId = 1L;
        MemberRefrigerator memberRefrigerator1 = createMockMemberRefrigerator(10L, "Fridge1", RefrigeratorRole.OWNER);
        MemberRefrigerator memberRefrigerator2 = createMockMemberRefrigerator(20L, "Fridge2", RefrigeratorRole.MEMBER);

        given(memberRefrigeratorFinder.findAllByMemberId(memberId))
            .willReturn(List.of(memberRefrigerator1, memberRefrigerator2));

        // when
        var responses = refrigeratorService.findAllMyRefrigerators(memberId);

        // then
        assertThat(responses).hasSize(2)
            .extracting("name", "role", "isOwner")
            .containsExactly(
                tuple("Fridge1", RefrigeratorRole.OWNER, true),
                tuple("Fridge2", RefrigeratorRole.MEMBER, false)
            );
    }

    @Test
    void 특정_냉장고의_상세_정보를_조회한다() {
        // given
        long memberId = 1L;
        long refrigeratorId = 10L;
        var key = new MemberRefrigeratorKey(memberId, refrigeratorId);

        MemberRefrigerator memberRefrigerator = createMockMemberRefrigerator(refrigeratorId, "MyFridge", RefrigeratorRole.OWNER);
        given(memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(memberId, refrigeratorId))
            .willReturn(memberRefrigerator);

        // when
        var response = refrigeratorService.findRefrigerator(key);

        // then
        assertThat(response).isNotNull()
            .extracting("name", "role", "isOwner")
            .containsExactly("MyFridge", RefrigeratorRole.OWNER, true);
    }

    private MemberRefrigerator createMockMemberRefrigerator(long id, String name, RefrigeratorRole role) {
        MemberRefrigerator memberRefrigerator = mock(MemberRefrigerator.class);
        Refrigerator refrigerator = mock(Refrigerator.class);

        given(memberRefrigerator.getRole()).willReturn(role);
        given(memberRefrigerator.isOwner()).willReturn(role == RefrigeratorRole.OWNER);
        given(memberRefrigerator.getRefrigerator()).willReturn(refrigerator);
        given(refrigerator.getId()).willReturn(id);
        given(refrigerator.getName()).willReturn(name);

        return memberRefrigerator;
    }

}