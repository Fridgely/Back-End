package soon.fridgely.domain.refrigerator.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "초대 코드로 냉장고 참여 요청")
public record InvitationCodeJoinRequest(

    @Schema(description = "8자리 초대 코드", example = "ABC12345")
    @NotBlank(message = "초대 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "초대 코드는 8자리 문자열이어야 합니다.")
    String code

) {
}