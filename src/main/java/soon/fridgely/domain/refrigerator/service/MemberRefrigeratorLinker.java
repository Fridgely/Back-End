package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;

@RequiredArgsConstructor
@Component
public class MemberRefrigeratorLinker {

    private final MemberRefrigeratorRepository memberRefrigeratorRepository;

    public void linkToOwner(Member member, Refrigerator refrigerator) {
        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);
    }

}