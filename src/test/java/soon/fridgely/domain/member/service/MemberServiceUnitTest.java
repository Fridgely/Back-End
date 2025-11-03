package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberManager memberManager;

    @Test
    void 회원을_등록한다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(1L);

        given(memberManager.register(any(MemberInfo.class)))
            .willReturn(mockMember);

        // when
        Long memberId = memberService.register(memberInfo);

        // then
        then(memberManager).should(times(1)).register(memberInfo);
        assertThat(memberId).isEqualTo(1L);
    }

}