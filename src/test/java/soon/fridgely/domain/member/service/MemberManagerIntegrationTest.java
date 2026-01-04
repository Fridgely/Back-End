package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.IntegrationTestSupport;
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
        var memberInfo = fixtureMonkey.giveMeBuilder(MemberInfo.class)
            .set("loginId", "testId")
            .set("password", "testPassword")
            .set("nickname", "testNickname")
            .sample();

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
        var memberInfo = fixtureMonkey.giveMeBuilder(MemberInfo.class)
            .set("loginId", "testId")
            .set("password", "testPassword")
            .set("nickname", "testNickname")
            .sample();

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
        var memberInfo1 = fixtureMonkey.giveMeBuilder(MemberInfo.class)
            .set("loginId", "duplicateId")
            .set("password", "testPassword1")
            .set("nickname", "testNickname1")
            .sample();
        var memberInfo2 = fixtureMonkey.giveMeBuilder(MemberInfo.class)
            .set("loginId", "duplicateId")
            .set("password", "testPassword2")
            .set("nickname", "testNickname2")
            .sample();

        // when
        memberManager.register(memberInfo1);

        // then
        assertThatThrownBy(() -> memberManager.register(memberInfo2))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_LOGIN_ID);
    }

}