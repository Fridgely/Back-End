package soon.fridgely.domain.refrigerator.dto.command;

import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

public record CachedRefrigeratorInfo(
    long id,
    String name,
    RefrigeratorRole role,
    boolean isOwner
) {

    public static CachedRefrigeratorInfo from(MemberRefrigerator memberRefrigerator) {
        return new CachedRefrigeratorInfo(
            memberRefrigerator.getRefrigerator().getId(),
            memberRefrigerator.getRefrigerator().getName(),
            memberRefrigerator.getRole(),
            memberRefrigerator.isOwner()
        );
    }

}