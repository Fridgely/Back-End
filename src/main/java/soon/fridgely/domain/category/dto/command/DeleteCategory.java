package soon.fridgely.domain.category.dto.command;

import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;

public record DeleteCategory(
    long memberId,
    long refrigeratorId,
    long categoryId
) {

    public MemberRefrigeratorKey toKey() {
        return new MemberRefrigeratorKey(memberId, refrigeratorId);
    }

}