package soon.fridgely.global.security.dto.response;

public record TokenResponse(
    String grantType,
    String accessToken,
    String refreshToken
) {

    private static final String GRANT_TYPE = "Bearer";

    public TokenResponse(String accessToken, String refreshToken) {
        this(GRANT_TYPE, accessToken, refreshToken);
    }

}