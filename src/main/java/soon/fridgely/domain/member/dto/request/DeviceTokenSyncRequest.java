package soon.fridgely.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "디바이스 토큰 동기화 요청")
public record DeviceTokenSyncRequest(

    @Schema(description = "FCM 디바이스 토큰", example = "dGVzdF90b2tlbl9leGFtcGxl...")
    @NotBlank(message = "토큰은 필수입니다.")
    String token

) {
}