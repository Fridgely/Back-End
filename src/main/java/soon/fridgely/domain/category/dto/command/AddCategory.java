package soon.fridgely.domain.category.dto.command;

import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;

public record AddCategory(
    String name,
    long refrigeratorId,
    long memberId
) {

    public MemberRefrigeratorKey toKey() {
        return new MemberRefrigeratorKey(refrigeratorId, memberId);
    }

}