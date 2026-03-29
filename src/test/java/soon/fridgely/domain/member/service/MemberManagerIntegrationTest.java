package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.ImageManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class MemberManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberManager memberManager;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private ImageManager imageManager;

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
    void 프로필_이미지가_없는_회원의_이미지를_업데이트한다() {
        // given
        Member saved = memberRepository.save(
            member(fixtureMonkey).setNull("profileImageUrl").sample()
        );
        String newImageUrl = "https://s3.amazonaws.com/bucket/images/new-profile.jpg";

        // when
        memberManager.updateProfileImage(saved.getId(), newImageUrl);

        // then
        Member updated = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(newImageUrl);
        then(imageManager).should(never()).delete(any());
    }

    @Test
    void 기존_프로필_이미지가_있는_회원의_이미지를_교체하면_삭제_이벤트가_처리된다() {
        // given
        String oldImageUrl = "https://s3.amazonaws.com/bucket/images/old-profile.jpg";
        String newImageUrl = "https://s3.amazonaws.com/bucket/images/new-profile.jpg";
        Member saved = memberRepository.save(
            member(fixtureMonkey).set("profileImageUrl", oldImageUrl).sample()
        );

        // when
        memberManager.updateProfileImage(saved.getId(), newImageUrl);

        // then
        Member updated = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(newImageUrl);
        then(imageManager).should(times(1)).delete(oldImageUrl);
    }

    @Test
    void 존재하지_않는_회원의_프로필_이미지를_업데이트하면_예외가_발생한다() {
        // given
        long nonExistentMemberId = 999L;

        // expected
        assertThatThrownBy(() -> memberManager.updateProfileImage(nonExistentMemberId, "https://s3.amazonaws.com/bucket/images/image.jpg"))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
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