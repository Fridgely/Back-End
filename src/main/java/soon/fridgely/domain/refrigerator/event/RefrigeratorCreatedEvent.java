package soon.fridgely.domain.refrigerator.event;

import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;

/**
 * 냉장고 생성 완료 이벤트
 *
 * @param refrigeratorId 생성된 냉장고 ID
 * @param memberId       냉장고 소유자(회원) ID
 */
public record RefrigeratorCreatedEvent(
    long refrigeratorId,
    long memberId
) {

    public MemberRefrigeratorKey toKey() {
        return new MemberRefrigeratorKey(memberId, refrigeratorId);
    }

}