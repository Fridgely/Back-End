package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
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
            .extracting("notificationTime", "daysBeforeExpiration", "enabled", "member.id")
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

    private Member createMember(String loginId) {
        return Member.builder()
            .loginId(loginId)
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}