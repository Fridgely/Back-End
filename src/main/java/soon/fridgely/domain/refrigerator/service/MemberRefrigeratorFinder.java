package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.refrigerator.dto.command.CachedMemberRefrigerators;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MemberRefrigeratorFinder {

    private final MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Cacheable(value = "myRefrigerators", key = "#memberId")
    @Transactional(readOnly = true)
    public CachedMemberRefrigerators findAllByMemberId(long memberId) {
        List<MemberRefrigerator> refrigerators = memberRefrigeratorRepository.findAllMyRefrigerators(memberId, EntityStatus.ACTIVE);
        return CachedMemberRefrigerators.from(memberId, refrigerators);
    }

    @Transactional(readOnly = true)
    public MemberRefrigerator findByMemberIdAndRefrigeratorId(long memberId, long refrigeratorId) {
        return memberRefrigeratorRepository.findByMemberIdAndRefrigeratorId(memberId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public boolean existsByRefrigeratorIdAndMemberId(long refrigeratorId, long memberId) {
        return memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(refrigeratorId, memberId, EntityStatus.ACTIVE);
    }

}