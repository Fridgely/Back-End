package soon.fridgely.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.member.dto.commmand.MemberInfo;

public record MemberRegisterRequest(

    @NotBlank(message = "ID는 필수입니다.")
    String loginId,

    @NotBlank(message = "비밀번호는 필수입니다.")
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    String nickname

) {

    public MemberInfo toInfo() {
        return new MemberInfo(this.loginId, this.password, this.nickname);
    }

}