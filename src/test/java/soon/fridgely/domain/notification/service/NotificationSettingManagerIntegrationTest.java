package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSettingManagerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationSettingManager notificationSettingManager;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 기본_알림_설정을_생성한다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        // when
        notificationSettingManager.createDefaultSetting(member);

        // then
        NotificationSetting setting = notificationSettingRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(setting)
            .extracting("alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled", "member.id")
            .containsExactly(LocalTime.of(9, 0), 3, true, member.getId());
    }

    @Test
    void 이미_설정이_존재하면_새로_생성하지_않는다() {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        // when
        notificationSettingManager.createDefaultSetting(member);
        long countBefore = notificationSettingRepository.count();

        notificationSettingManager.createDefaultSetting(member);
        long countAfter = notificationSettingRepository.count();

        // then
        assertThat(countBefore).isEqualTo(countAfter);
    }

    @Test
    void 다른_회원에_대해서는_각각_설정이_생성된다() {
        // given
        Member member1 = createMember("testId1");
        Member member2 = createMember("testId2");
        memberRepository.saveAll(List.of(member1, member2));

        // when
        notificationSettingManager.createDefaultSetting(member1);
        notificationSettingManager.createDefaultSetting(member2);

        // then
        NotificationSetting setting1 = notificationSettingRepository.findByMemberId(member1.getId()).orElseThrow();
        assertThat(setting1).isNotNull();

        NotificationSetting setting2 = notificationSettingRepository.findByMemberId(member2.getId()).orElseThrow();
        assertThat(setting2).isNotNull();

        long savedCount = notificationSettingRepository.count();
        assertThat(savedCount).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, true",
        "9, 0, 5, false",
        "14, 30, 15, true",
        "23, 59, 30, false"
    })
    void 알림_설정을_수정한다(int hour, int minute, int days, boolean enabled) {
        // given
        Member member = createMember("testId");
        memberRepository.save(member);

        NotificationSetting defaultSetting = NotificationSetting.createDefaultSetting(member);
        notificationSettingRepository.save(defaultSetting);

        AlertSchedule newSchedule = AlertSchedule.of(LocalTime.of(hour, minute), days);

        // when
        notificationSettingManager.update(member.getId(), newSchedule, enabled);

        // then
        NotificationSetting setting = notificationSettingRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(setting).isNotNull()
            .extracting("alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled")
            .containsExactly(LocalTime.of(hour, minute), days, enabled);
    }

    private Member createMember(String loginId) {
        return Member.builder()
            .loginId(loginId)
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}