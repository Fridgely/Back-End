package soon.fridgely.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.auth.dto.command.LoginInfo;

@Schema(description = "로그인 요청")
public record LoginRequest(

    @Schema(description = "사용자 로그인 ID", example = "user123")
    @NotBlank(message = "ID는 필수입니다.")
    String loginId,

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password

) {

    public LoginInfo toLoginInfo() {
        return new LoginInfo(this.loginId, this.password);
    }

}