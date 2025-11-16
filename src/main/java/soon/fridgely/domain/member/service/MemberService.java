package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.dto.command.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.event.RefrigeratorCreatedEvent;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberManager memberManager;
    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원 가입 후 기본 냉장고 생성 및 연결 완료 후 기본 카테고리 생성 이벤트 발행
     *
     * @param memberInfo 회원 정보
     * @return 생성된 회원 ID
     */
    @Transactional
    public Long register(MemberInfo memberInfo) {
        Member member = memberManager.register(memberInfo);
        Refrigerator refrigerator = refrigeratorManager.register(member);
        memberRefrigeratorLinker.linkToOwner(member, refrigerator);

        // 냉장고 생성 완료 후 Default category 생성
        eventPublisher.publishEvent(new RefrigeratorCreatedEvent(refrigerator.getId(), member.getId()));

        return member.getId();
    }

}