package soon.fridgely.domain.refrigerator.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "초대 코드 응답")
public record InvitationCodeResponse(

    @Schema(description = "8자리 초대 코드", example = "ABC12345")
    String code,

    @Schema(description = "초대 코드 만료 시간", example = "2026-01-08T23:59:59")
    LocalDateTime expirationAt

) {
}