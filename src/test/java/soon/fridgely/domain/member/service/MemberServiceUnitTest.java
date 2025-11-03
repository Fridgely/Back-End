package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberManager memberManager;

    @Mock
    private RefrigeratorManager refrigeratorManager;

    @Mock
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Test
    void 회원을_등록하고_기본_냉장고를_생성한_뒤_연결한다() { // 4. 테스트 이름 구체화
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");
        Member mockMember = mock(Member.class);
        Refrigerator mockRefrigerator = mock(Refrigerator.class);

        given(mockMember.getId()).willReturn(1L);

        given(memberManager.register(any(MemberInfo.class)))
            .willReturn(mockMember);
        given(refrigeratorManager.register(any(Member.class)))
            .willReturn(mockRefrigerator);

        // when
        Long memberId = memberService.register(memberInfo);

        // then
        InOrder inOrder = inOrder(memberManager, refrigeratorManager, memberRefrigeratorLinker);

        then(memberManager).should(inOrder).register(memberInfo);
        then(refrigeratorManager).should(inOrder).register(mockMember);
        then(memberRefrigeratorLinker).should(inOrder).linkToOwner(mockMember, mockRefrigerator);

        assertThat(memberId).isEqualTo(1L);
    }

}