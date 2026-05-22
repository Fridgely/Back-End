package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.NotificationSettingFixture.notificationSetting;

class NotificationSettingServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationSettingService notificationSettingService;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
    }

    @Test
    void 회원_가입_시_기본_알림_설정이_생성된다() {
        // when
        notificationSettingService.createDefaultSetting(member);

        // then
        NotificationSetting setting = notificationSettingRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(setting)
            .extracting("alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled", "member.id")
            .containsExactly(LocalTime.of(9, 0), 3, true, member.getId());
    }

    @Test
    void 이미_알림_설정이_존재하면_중복_생성하지_않는다() {
        // when
        notificationSettingService.createDefaultSetting(member);
        long countBefore = notificationSettingRepository.count();

        notificationSettingService.createDefaultSetting(member);
        long countAfter = notificationSettingRepository.count();

        // then
        assertThat(countBefore).isEqualTo(countAfter);
    }

    @Test
    void 회원마다_독립적으로_알림_설정이_생성된다() {
        // given
        Member member2 = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        // when
        notificationSettingService.createDefaultSetting(member);
        notificationSettingService.createDefaultSetting(member2);

        // then
        assertThat(notificationSettingRepository.findByMemberId(member.getId())).isPresent();
        assertThat(notificationSettingRepository.findByMemberId(member2.getId())).isPresent();
        assertThat(notificationSettingRepository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, true",
        "9, 0, 5, false",
        "14, 30, 15, true",
        "23, 59, 30, false"
    })
    void 알림_설정을_수정하면_변경_사항이_저장된다(int hour, int minute, int days, boolean enabled) {
        // given
        notificationSettingRepository.save(
            notificationSetting(fixtureMonkey, member).sample()
        );

        var request = new NotificationSettingUpdateRequest(LocalTime.of(hour, minute), days, enabled);

        // when
        notificationSettingService.updateNotificationSetting(member.getId(), request);

        // then
        NotificationSetting setting = notificationSettingRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(setting)
            .extracting("alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled")
            .containsExactly(LocalTime.of(hour, minute), days, enabled);
    }

}
