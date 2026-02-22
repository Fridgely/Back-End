package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class RefrigeratorManager {

    private final RefrigeratorRepository refrigeratorRepository;

    public Refrigerator register(Member member) {
        Refrigerator register = Refrigerator.register(member.getNickname());
        return refrigeratorRepository.save(register);
    }

    @CacheEvict(value = "myRefrigerators", allEntries = true)
    @Transactional
    public void update(long refrigeratorId, String newName) {
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        refrigerator.update(newName);
    }

    @Transactional
    public InvitationCode refreshInvitationCode(long refrigeratorId, String newCode, LocalDateTime now) {
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));

        InvitationCode invitationCode = InvitationCode.generate(newCode, now);
        refrigerator.refreshInvitationCode(invitationCode);
        return invitationCode;
    }

    @Transactional(readOnly = true)
    public Refrigerator findByInvitationCode(String code) {
        return refrigeratorRepository.findByInvitationCode_code(code)
            .filter(r -> r.getStatus() == EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.INVALID_INVITATION_CODE));
    }

}