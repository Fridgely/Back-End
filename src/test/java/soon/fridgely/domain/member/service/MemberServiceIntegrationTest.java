package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;
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

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Test
    void 회원가입_시_회원_기본_냉장고_알림_설정_연결이_모두_생성된다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        // when
        long memberId = memberService.register(memberInfo);

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
            .extracting("notificationTime", "daysBeforeExpiration", "enabled", "member.id")
            .containsExactly(LocalTime.of(9, 0), 3, true, memberId);
    }

}