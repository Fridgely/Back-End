package soon.fridgely.domain.refrigerator.dto.response;

import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

public record RefrigeratorResponse(
    long id,
    String name,
    RefrigeratorRole role,
    boolean isOwner
) {

    public static RefrigeratorResponse from(MemberRefrigerator memberRefrigerator) {
        Refrigerator refrigerator = memberRefrigerator.getRefrigerator();
        return new RefrigeratorResponse(
            refrigerator.getId(),
            refrigerator.getName(),
            memberRefrigerator.getRole(),
            memberRefrigerator.isOwner()
        );
    }

}