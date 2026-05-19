package soon.fridgely.domain.member.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.RefrigeratorService;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberFacadeUnitTest {

    @InjectMocks
    private MemberFacade memberFacade;

    @Mock
    private MemberService memberService;

    @Mock
    private RefrigeratorService refrigeratorService;

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

        given(memberService.register(any(MemberInfo.class))).willReturn(mockMember);
        given(refrigeratorService.register(any(Member.class))).willReturn(mockRefrigerator);

        // when
        Long memberId = memberFacade.register(memberInfo);

        // then
        then(memberService).should().register(memberInfo);
        then(notificationSettingManager).should().createDefaultSetting(mockMember);
        then(refrigeratorService).should().register(mockMember);
        then(refrigeratorService).should().linkToOwner(mockMember, mockRefrigerator);
        then(eventPublisher).should().publishEvent(any(RefrigeratorCreatedEvent.class));

        assertThat(memberId).isEqualTo(1L);
    }
}
