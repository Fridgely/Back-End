package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    private InvitationCodeGenerator codeGenerator;

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

}