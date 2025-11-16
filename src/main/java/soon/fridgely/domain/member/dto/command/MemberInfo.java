package soon.fridgely.domain.member.dto.command;

/**
 * 회원 가입 및 정보 수정을 위한 DTO
 *
 * @param loginId  회원의 로그인 아이디
 * @param password 회원의 비밀번호 (평문)
 * @param nickname 회원의 닉네임
 */
public record MemberInfo(
    String loginId,
    String password,
    String nickname
) {
}