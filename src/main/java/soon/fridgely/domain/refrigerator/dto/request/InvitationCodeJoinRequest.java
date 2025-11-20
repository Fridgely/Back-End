package soon.fridgely.domain.refrigerator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InvitationCodeJoinRequest(

    @NotBlank(message = "초대 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "초대 코드는 8자리 문자열이어야 합니다.")
    String code

) {
}