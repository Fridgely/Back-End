package soon.fridgely.domain.member.dto.response;

import soon.fridgely.domain.member.entity.Member;

public record MemberProfileResponse(
    String loginId,
    String nickname,
    String profileImageUrl
) {

    public static MemberProfileResponse of(Member member) {
        return new MemberProfileResponse(
            member.getLoginId(),
            member.getNickname(),
            member.getProfileImageUrl()
        );
    }

}