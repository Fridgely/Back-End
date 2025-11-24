package soon.fridgely.domain.refrigerator.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorFinder;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class RefrigeratorAccessValidator {

    private final MemberRefrigeratorFinder memberRefrigeratorFinder;

    /*
     * 냉장고에 속한 멤버인지 검증
     * 실패 시 AUTHORIZATION_FAILED(403) 예외 발생
     */
    public void validateMembership(MemberRefrigeratorKey key) {
        boolean isMember = memberRefrigeratorFinder.existsByRefrigeratorIdAndMemberId(key.refrigeratorId(), key.memberId());
        if (!isMember) {
            throw new CoreException(ErrorType.AUTHORIZATION_FAILED);
        }
    }

}