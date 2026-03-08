package soon.fridgely.domain.refrigerator.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

@Schema(description = "냉장고 팀원 정보 응답")
public record RefrigeratorMemberResponse(

    @Schema(description = "팀원 ID", example = "1")
    long memberId,

    @Schema(description = "팀원 닉네임", example = "홍길동")
    String nickname,

    @Schema(description = "냉장고 역할", example = "MEMBER")
    RefrigeratorRole role,

    @Schema(description = "소유자 여부", example = "false")
    boolean isOwner

) {

    public static RefrigeratorMemberResponse from(MemberRefrigerator memberRefrigerator) {
        Member member = memberRefrigerator.getMember();
        return new RefrigeratorMemberResponse(
            member.getId(),
            member.getNickname(),
            memberRefrigerator.getRole(),
            memberRefrigerator.isOwner()
        );
    }

}

