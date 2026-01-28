package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class MemberRefrigeratorLinker {

    private final MemberRefrigeratorRepository memberRefrigeratorRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final MemberRepository memberRepository;

    @CacheEvict(value = "myRefrigerators", key = "#member.id")
    public void linkToOwner(Member member, Refrigerator refrigerator) {
        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);
    }

    @CacheEvict(value = "myRefrigerators", key = "#key.memberId()")
    @Transactional
    public void linkMemberToRefrigerator(MemberRefrigeratorKey key, RefrigeratorRole role) {
        if (memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(key.refrigeratorId(), key.memberId(), EntityStatus.ACTIVE)) {
            throw new CoreException(ErrorType.ALREADY_JOINED_REFRIGERATOR);
        }

        Member member = memberRepository.findById(key.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Refrigerator refrigerator = refrigeratorRepository.findById(key.refrigeratorId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        memberRefrigeratorRepository.save(MemberRefrigerator.link(member, refrigerator, role));
    }

}