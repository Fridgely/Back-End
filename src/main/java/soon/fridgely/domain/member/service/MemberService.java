package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberManager memberManager;

    public Long register(MemberInfo memberInfo) {
        Member member = memberManager.register(memberInfo);
        return member.getId();
    }

}