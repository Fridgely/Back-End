package soon.fridgely.domain.auth.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueTokenRequest(

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    String refreshToken

) {
}