package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.service.NotificationSettingManager;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.RefrigeratorService;

@RequiredArgsConstructor
@Service
public class MemberFacade {

    private final MemberService memberService;
    private final RefrigeratorService refrigeratorService;
    private final NotificationSettingManager notificationSettingManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long register(MemberInfo memberInfo) {
        Member member = memberService.register(memberInfo);
        notificationSettingManager.createDefaultSetting(member);
        Refrigerator refrigerator = refrigeratorService.register(member);
        refrigeratorService.linkToOwner(member, refrigerator);
        eventPublisher.publishEvent(new RefrigeratorCreatedEvent(refrigerator.getId(), member.getId()));
        return member.getId();
    }
}
