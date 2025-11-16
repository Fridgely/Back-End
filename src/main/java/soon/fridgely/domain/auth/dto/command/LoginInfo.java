package soon.fridgely.domain.auth.dto.command;

/**
 * 로그인을 위한 회원 정보 DTO
 *
 * @param loginId  회원의 로그인 아이디
 * @param password 회원의 비밀번호 (평문)
 */
public record LoginInfo(
    String loginId,
    String password
) {
}