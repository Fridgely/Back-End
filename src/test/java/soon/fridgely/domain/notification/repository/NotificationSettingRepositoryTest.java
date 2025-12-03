package soon.fridgely.domain.notification.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSettingRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @ParameterizedTest
    @MethodSource("provideTimesForScheduledAlerts")
    void 특정_시간대에_활성화된_알림_설정을_조회한다(LocalTime startTime, LocalTime endTime, int expectedCount) {
        // given
        Member member1 = createMember("user1");
        Member member2 = createMember("user2");
        Member member3 = createMember("user3");
        memberRepository.saveAll(List.of(member1, member2, member3));

        NotificationSetting setting1 = createNotificationSetting(member1, LocalTime.of(9, 0), true);
        NotificationSetting setting2 = createNotificationSetting(member2, LocalTime.of(9, 30), true);
        NotificationSetting setting3 = createNotificationSetting(member3, LocalTime.of(10, 0), true);
        notificationSettingRepository.saveAll(List.of(setting1, setting2, setting3));

        // when
        List<NotificationSetting> settings = notificationSettingRepository.findAllActiveByTime(startTime, endTime);

        // then
        assertThat(settings).hasSize(expectedCount);
    }

    @Test
    void 비활성화된_알림_설정은_조회되지_않는다() {
        // given
        Member member1 = createMember("user1");
        Member member2 = createMember("user2");
        memberRepository.saveAll(List.of(member1, member2));

        NotificationSetting activeSetting = createNotificationSetting(member1, LocalTime.of(9, 0), true);
        NotificationSetting inactiveSetting = createNotificationSetting(member2, LocalTime.of(9, 30), false);
        notificationSettingRepository.saveAll(List.of(activeSetting, inactiveSetting));

        // when
        List<NotificationSetting> settings = notificationSettingRepository.findAllActiveByTime(
            LocalTime.of(9, 0),
            LocalTime.of(9, 59)
        );

        // then
        assertThat(settings).hasSize(1);
        assertThat(settings.get(0).getMember().getId()).isEqualTo(member1.getId());
    }

    @Test
    void 시간_범위에_해당하는_알림_설정이_없으면_빈_리스트를_반환한다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        NotificationSetting setting = createNotificationSetting(member, LocalTime.of(9, 0), true);
        notificationSettingRepository.save(setting);

        // when
        List<NotificationSetting> settings = notificationSettingRepository.findAllActiveByTime(
            LocalTime.of(10, 0),
            LocalTime.of(10, 59)
        );

        // then
        assertThat(settings).isEmpty();
    }

    private Member createMember(String loginId) {
        return Member.builder()
            .loginId(loginId)
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private static Stream<Arguments> provideTimesForScheduledAlerts() {
        return Stream.of(
            Arguments.of(LocalTime.of(9, 0), LocalTime.of(9, 59), 2),
            Arguments.of(LocalTime.of(9, 0), LocalTime.of(9, 29), 1),
            Arguments.of(LocalTime.of(10, 0), LocalTime.of(10, 59), 1),
            Arguments.of(LocalTime.of(8, 0), LocalTime.of(8, 59), 0)
        );
    }

    private NotificationSetting createNotificationSetting(Member member, LocalTime time, boolean enabled) {
        AlertSchedule schedule = AlertSchedule.of(time, 3);
        return NotificationSetting.builder()
            .member(member)
            .alertSchedule(schedule)
            .enabled(enabled)
            .build();
    }

}