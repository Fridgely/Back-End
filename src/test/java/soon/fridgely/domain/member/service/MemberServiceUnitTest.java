package soon.fridgely.domain.member.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

}