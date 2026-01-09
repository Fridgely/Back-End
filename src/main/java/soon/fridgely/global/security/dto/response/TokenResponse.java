package soon.fridgely.global.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(

    @Schema(description = "인증 타입", example = "Bearer")
    String grantType,

    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken

) {

    private static final String GRANT_TYPE = "Bearer";

    public TokenResponse(String accessToken, String refreshToken) {
        this(GRANT_TYPE, accessToken, refreshToken);
    }

}