package soon.fridgely.domain.member.dto;

public record MemberInfo(
    String loginId,
    String password,
    String nickname
) {
}