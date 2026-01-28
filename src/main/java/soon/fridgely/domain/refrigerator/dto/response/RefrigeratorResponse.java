package soon.fridgely.domain.refrigerator.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.refrigerator.dto.command.CachedRefrigeratorInfo;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

@Schema(description = "냉장고 정보 응답")
public record RefrigeratorResponse(

    @Schema(description = "냉장고 ID", example = "1")
    long id,

    @Schema(description = "냉장고 이름", example = "우리집 냉장고")
    String name,

    @Schema(description = "사용자의 냉장고 역할", example = "OWNER")
    RefrigeratorRole role,

    @Schema(description = "소유자 여부", example = "true")
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

    public static RefrigeratorResponse from(CachedRefrigeratorInfo info) {
        return new RefrigeratorResponse(
            info.id(),
            info.name(),
            info.role(),
            info.isOwner()
        );
    }

}