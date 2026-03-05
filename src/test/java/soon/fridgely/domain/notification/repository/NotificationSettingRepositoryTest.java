package soon.fridgely.domain.notification.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.NotificationSettingFixture.notificationSetting;

class NotificationSettingRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @ParameterizedTest
    @MethodSource("provideTimesForScheduledAlerts")
    void 특정_시간대에_활성화된_알림_설정을_조회한다(LocalTime startTime, LocalTime endTime, int expectedCount) {
        // given
        Member member1 = member(fixtureMonkey).sample();
        Member member2 = member(fixtureMonkey).sample();
        Member member3 = member(fixtureMonkey).sample();
        memberRepository.saveAll(List.of(member1, member2, member3));

        notificationSettingRepository.saveAll(List.of(
            notificationSetting(fixtureMonkey, member1)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
                .sample(),
            notificationSetting(fixtureMonkey, member2)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 30), 5))
                .sample(),
            notificationSetting(fixtureMonkey, member3)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(10, 0), 1))
                .sample()
        ));

        // when
        Slice<NotificationSetting> slice = notificationSettingRepository.findAllActiveByTimeWithCursor(
            startTime,
            endTime,
            Long.MAX_VALUE,
            PageRequest.of(0, 10)
        );

        // then
        assertThat(slice.getContent()).hasSize(expectedCount);
    }

    @Test
    void 비활성화된_알림_설정은_조회되지_않는다() {
        // given
        Member member1 = member(fixtureMonkey).sample();
        Member member2 = member(fixtureMonkey).sample();
        memberRepository.saveAll(List.of(member1, member2));

        notificationSettingRepository.saveAll(List.of(
            notificationSetting(fixtureMonkey, member1)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
                .sample(),
            notificationSetting(fixtureMonkey, member2)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 30), 5))
                .set("enabled", false)
                .sample()
        ));

        // when
        Slice<NotificationSetting> slice = notificationSettingRepository.findAllActiveByTimeWithCursor(
            LocalTime.of(9, 0),
            LocalTime.of(9, 59),
            Long.MAX_VALUE,
            PageRequest.of(0, 10)
        );

        // then
        List<NotificationSetting> result = slice.getContent();
        assertThat(result).hasSize(1)
            .extracting("member.id")
            .containsExactly(member1.getId());
    }

    @Test
    void cursor_기반_페이징이_ID_내림차순으로_조회된다() {
        // given
        Member member1 = member(fixtureMonkey).sample();
        Member member2 = member(fixtureMonkey).sample();
        Member member3 = member(fixtureMonkey).sample();
        memberRepository.saveAll(List.of(member1, member2, member3));

        NotificationSetting setting1 = notificationSetting(fixtureMonkey, member1).sample();
        NotificationSetting setting2 = notificationSetting(fixtureMonkey, member2).sample();
        NotificationSetting setting3 = notificationSetting(fixtureMonkey, member3).sample();
        notificationSettingRepository.saveAll(List.of(setting1, setting2, setting3));

        // when
        Slice<NotificationSetting> firstPage = notificationSettingRepository.findAllActiveByTimeWithCursor(
            LocalTime.of(9, 0),
            LocalTime.of(9, 59),
            Long.MAX_VALUE,
            PageRequest.of(0, 2)
        );

        Long lastId = firstPage.getContent().get(1).getId();
        Slice<NotificationSetting> secondPage = notificationSettingRepository.findAllActiveByTimeWithCursor(
            LocalTime.of(9, 0),
            LocalTime.of(9, 59),
            lastId,
            PageRequest.of(0, 2)
        );

        // then
        assertThat(firstPage.getContent()).hasSize(2)
            .extracting("id")
            .containsExactly(setting3.getId(), setting2.getId());
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.getContent()).hasSize(1)
            .extracting("id")
            .containsExactly(setting1.getId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 시간_범위에_해당하는_알림_설정이_없으면_빈_Slice를_반환한다() {
        // given
        Member member = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        notificationSettingRepository.save(
            notificationSetting(fixtureMonkey, member)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(10, 0), 3))
                .sample()
        );

        // when
        Slice<NotificationSetting> slice = notificationSettingRepository.findAllActiveByTimeWithCursor(
            LocalTime.of(9, 0),
            LocalTime.of(9, 59),
            Long.MAX_VALUE,
            PageRequest.of(0, 10)
        );

        // then
        assertThat(slice.getContent()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
    }

    private static Stream<Arguments> provideTimesForScheduledAlerts() {
        return Stream.of(
            Arguments.of(LocalTime.of(9, 0), LocalTime.of(9, 59), 2),
            Arguments.of(LocalTime.of(9, 0), LocalTime.of(9, 29), 1),
            Arguments.of(LocalTime.of(10, 0), LocalTime.of(10, 59), 1),
            Arguments.of(LocalTime.of(8, 0), LocalTime.of(8, 59), 0)
        );
    }

}