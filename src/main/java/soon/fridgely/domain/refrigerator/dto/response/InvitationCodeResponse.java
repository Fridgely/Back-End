package soon.fridgely.domain.refrigerator.dto.response;

import java.time.LocalDateTime;

public record InvitationCodeResponse(
    String code,
    LocalDateTime expirationAt
) {
}