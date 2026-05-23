package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.fake.FakeImageManager;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

@Import(MemberFacadeIntegrationTest.FakeConfig.class)
class MemberFacadeIntegrationTest extends IntegrationTestSupport {

    @TestConfiguration
    static class FakeConfig {
        @Bean
        @Primary
        public FakeImageManager fakeImageManager() {
            return new FakeImageManager();
        }
    }

    @Autowired
    private MemberFacade memberFacade;

    @Autowired
    private FakeImageManager fakeImageManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @BeforeEach
    void setUp() {
        fakeImageManager.reset();
    }

    @Test
    void 회원가입_시_회원_기본_냉장고_알림_설정_연결이_모두_생성된다() {
        // given
        var memberInfo = fixtureMonkey.giveMeBuilder(MemberInfo.class)
            .set("loginId", "testId")
            .set("password", "testPassword")
            .set("nickname", "testNickname")
            .sample();

        // when
        long memberId = memberFacade.register(memberInfo);

        // then
        Member member = memberRepository.findById(memberId).orElseThrow();
        assertThat(member)
            .extracting("loginId", "nickname")
            .containsExactly("testId", "testNickname");

        MemberRefrigerator memberRefrigerator = memberRefrigeratorRepository.findByMemberAndRoleAndStatus(member, RefrigeratorRole.OWNER, EntityStatus.ACTIVE)
            .stream()
            .findFirst()
            .orElseThrow();
        assertThat(memberRefrigerator)
            .extracting("member.id", "role")
            .containsExactly(memberId, RefrigeratorRole.OWNER);

        long refrigeratorId = memberRefrigerator.getRefrigerator().getId();
        Refrigerator refrigerator = refrigeratorRepository.findById(refrigeratorId).orElseThrow();
        assertThat(refrigerator.getName()).isEqualTo("testNickname의 냉장고");

        NotificationSetting notificationSetting = notificationSettingRepository.findByMemberId(memberId).orElseThrow();
        assertThat(notificationSetting)
            .extracting("alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled", "member.id")
            .containsExactly(LocalTime.of(9, 0), 3, true, memberId);
    }

    @Test
    void 프로필_이미지를_업로드하면_DB에_URL이_저장된다() {
        // given
        Member savedMember = memberRepository.save(member().sample());
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[1024]);
        String uploadedUrl = "https://s3.amazonaws.com/bucket/images/profile.jpg";
        fakeImageManager.setUploadResult(uploadedUrl);

        // when
        memberFacade.updateProfileImage(savedMember.getId(), file);

        // then
        Member updated = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(uploadedUrl);
    }

    @Test
    void DB_저장_실패_시_업로드된_이미지를_삭제한다() {
        // given
        long nonExistentMemberId = Long.MAX_VALUE;
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[1024]);
        String uploadedUrl = "https://s3.amazonaws.com/bucket/images/profile.jpg";
        fakeImageManager.setUploadResult(uploadedUrl);

        // when
        var actualException = catchThrowable(() -> memberFacade.updateProfileImage(nonExistentMemberId, file));

        // expected
        assertThat(actualException).isInstanceOf(CoreException.class);
        assertThat(fakeImageManager.getDeletedUrls()).contains(uploadedUrl);
    }

    @Test
    void 빈_파일로_프로필_이미지_업로드_시_예외가_발생한다() {
        // given
        Member savedMember = memberRepository.save(member().sample());
        MockMultipartFile emptyFile = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[0]);

        // when
        var actualException = catchThrowable(() -> memberFacade.updateProfileImage(savedMember.getId(), emptyFile));

        // expected
        assertThat(actualException)
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
        assertThat(fakeImageManager.getDeletedUrls()).isEmpty();
    }

}
