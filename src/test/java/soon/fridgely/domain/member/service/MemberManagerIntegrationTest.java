package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberManager memberManager;

    @Test
    void 회원을_생성한다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        // when
        Member member = memberManager.register(memberInfo);

        // then
        assertThat(member)
            .extracting("loginId", "nickname")
            .containsExactly("testId", "testNickname");
    }

    @Test
    void 회원을_생성할때_비밀번호는_암호화된다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        // when
        Member member = memberManager.register(memberInfo);

        // then
        assertThat(member.getPassword())
            .isNotNull()
            .isNotEqualTo("testPassword");
    }

    @Test
    void 중복된_아이디로_회원_생성을_시도하면_예외가_발생한다() {
        // given
        MemberInfo memberInfo1 = new MemberInfo("testId", "testPassword1", "testNickname1");
        MemberInfo memberInfo2 = new MemberInfo("testId", "testPassword2", "testNickname2");

        // when
        memberManager.register(memberInfo1);

        // then
        assertThatThrownBy(() -> memberManager.register(memberInfo2))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_LOGIN_ID);
    }

}