package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;
import soon.fridgely.domain.refrigerator.service.RefrigeratorManager;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberManager memberManager;
    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;

    // 회원 가입과 동시에 기본 냉장고를 생성하고, 회원과 냉장고를 연결
    @Transactional
    public Long register(MemberInfo memberInfo) {
        Member member = memberManager.register(memberInfo);
        Refrigerator register = refrigeratorManager.register(member);
        memberRefrigeratorLinker.linkToOwner(member, register);

        return member.getId();
    }

}