package soon.fridgely.domain.member.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.image.ImageManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

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

    @Mock
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private MemberDeviceManager memberDeviceManager;

    @Mock
    private ImageManager imageManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 회원을_등록하고_기본_냉장고를_생성한_뒤_연결하고_이벤트를_발행한다() {
        // given
        var memberInfo = fixtureMonkey.giveMeOne(MemberInfo.class);
        Member mockMember = fixtureMonkey.giveMeBuilder(Member.class)
            .set("id", 1L)
            .sample();
        Refrigerator mockRefrigerator = fixtureMonkey.giveMeOne(Refrigerator.class);

        given(memberManager.register(any(MemberInfo.class))).willReturn(mockMember);
        given(refrigeratorManager.register(any(Member.class))).willReturn(mockRefrigerator);

        // when
        Long memberId = memberService.register(memberInfo);

        // then
        InOrder inOrder = inOrder(memberManager, notificationSettingManager, refrigeratorManager, memberRefrigeratorLinker, eventPublisher);

        then(memberManager).should(inOrder)
            .register(memberInfo);
        then(notificationSettingManager).should(inOrder)
            .createDefaultSetting(mockMember);
        then(refrigeratorManager).should(inOrder)
            .register(mockMember);
        then(memberRefrigeratorLinker).should(inOrder)
            .linkToOwner(mockMember, mockRefrigerator);
        then(eventPublisher).should(inOrder)
            .publishEvent(any(RefrigeratorCreatedEvent.class));

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void 마이페이지_프로필을_조회한다() {
        // given
        long memberId = 1L;
        Member mockMember = fixtureMonkey.giveMeBuilder(Member.class)
            .set("id", memberId)
            .set("loginId", "testId")
            .set("nickname", "testNickname")
            .set("profileImageUrl", "https://s3.amazonaws.com/bucket/images/profile.jpg")
            .sample();

        given(memberManager.findById(memberId)).willReturn(mockMember);

        // when
        var response = memberService.getMyProfile(memberId);

        // then
        then(memberManager).should().findById(memberId);
        assertThat(response.loginId()).isEqualTo("testId");
        assertThat(response.nickname()).isEqualTo("testNickname");
        assertThat(response.profileImageUrl()).isEqualTo("https://s3.amazonaws.com/bucket/images/profile.jpg");
    }

    @Test
    void 프로필_이미지가_없는_경우_profileImageUrl이_null이다() {
        // given
        long memberId = 1L;
        Member mockMember = fixtureMonkey.giveMeBuilder(Member.class)
            .set("id", memberId)
            .setNull("profileImageUrl")
            .sample();

        given(memberManager.findById(memberId)).willReturn(mockMember);

        // when
        var response = memberService.getMyProfile(memberId);

        // then
        assertThat(response.profileImageUrl()).isNull();
    }

    @Test
    void 프로필_이미지를_업로드한다() {
        // given
        long memberId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[1024]);
        String uploadedUrl = "https://s3.amazonaws.com/bucket/images/profile.jpg";

        given(imageManager.upload(file)).willReturn(uploadedUrl);

        // when
        memberService.updateProfileImage(memberId, file);

        // then
        InOrder inOrder = inOrder(imageManager, memberManager);
        then(imageManager).should(inOrder).upload(file);
        then(memberManager).should(inOrder).updateProfileImage(memberId, uploadedUrl);
    }

    @Test
    void DB_저장_실패_시_업로드된_이미지를_롤백한다() {
        // given
        long memberId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[1024]);
        String uploadedUrl = "https://s3.amazonaws.com/bucket/images/profile.jpg";

        given(imageManager.upload(file)).willReturn(uploadedUrl);
        willThrow(new RuntimeException("DB 오류"))
            .given(memberManager).updateProfileImage(memberId, uploadedUrl);

        // expected
        assertThatThrownBy(() -> memberService.updateProfileImage(memberId, file))
            .isInstanceOf(RuntimeException.class);

        then(imageManager).should().delete(uploadedUrl);
    }

}