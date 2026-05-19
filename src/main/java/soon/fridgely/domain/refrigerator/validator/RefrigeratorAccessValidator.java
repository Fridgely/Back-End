package soon.fridgely.domain.refrigerator.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class RefrigeratorAccessValidator {

    private final MemberRefrigeratorRepository memberRefrigeratorRepository;

    public void validateMembership(MemberRefrigeratorKey key) {
        boolean isMember = memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            key.refrigeratorId(), key.memberId(), EntityStatus.ACTIVE);
        if (!isMember) {
            throw new CoreException(ErrorType.AUTHORIZATION_FAILED);
        }
    }
}
