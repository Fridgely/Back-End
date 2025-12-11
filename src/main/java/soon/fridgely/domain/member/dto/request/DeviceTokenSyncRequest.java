package soon.fridgely.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenSyncRequest(

    @NotBlank(message = "토큰은 필수입니다.")
    String token

) {
}