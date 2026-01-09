package soon.fridgely.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.member.dto.command.MemberInfo;

@Schema(description = "회원 가입 요청")
public record MemberRegisterRequest(

    @Schema(description = "사용자 로그인 ID", example = "user123")
    @NotBlank(message = "ID는 필수입니다.")
    String loginId,

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password,

    @Schema(description = "닉네임", example = "홍길동")
    @NotBlank(message = "닉네임은 필수입니다.")
    String nickname

) {

    public MemberInfo toInfo() {
        return new MemberInfo(this.loginId, this.password, this.nickname);
    }

}