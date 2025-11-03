package soon.fridgely.domain.auth.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.auth.dto.LoginInfo;

public record LoginRequest(

    @NotBlank(message = "ID는 필수입니다.")
    String loginId,

    @NotBlank(message = "비밀번호는 필수입니다.")
    String password

) {

    public LoginInfo toLoginInfo() {
        return new LoginInfo(this.loginId, this.password);
    }

}