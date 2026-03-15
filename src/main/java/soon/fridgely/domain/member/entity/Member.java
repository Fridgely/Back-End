package soon.fridgely.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_members_login_id", columnNames = "loginId"),
        @UniqueConstraint(name = "uk_members_refresh_token", columnNames = "refreshToken")
    }
)
@Entity
public class Member extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(length = 512)
    private String refreshToken;

    public static Member register(
        String loginId,
        String password,
        String nickname,
        MemberRole role,
        PasswordEncoder encoder
    ) {
        return Member.builder()
            .loginId(requireNonNull(loginId, "loginId는 필수입니다."))
            .password(encoder.encode(requireNonNull(password, "password는 필수입니다.")))
            .nickname(requireNonNull(nickname, "nickname은 필수입니다."))
            .role(role)
            .build();
    }

    public void updateRefreshToken(String rawToken, PasswordEncoder encoder) {
        this.refreshToken = (rawToken == null)
            ? null
            : encoder.encode(sha256Hex(rawToken));
    }

    public boolean matchesRefreshToken(String rawToken, PasswordEncoder encoder) {
        return this.refreshToken != null && encoder.matches(sha256Hex(rawToken), this.refreshToken);
    }

    public String getRole() {
        return role.name();
    }

    /**
     * SHA-256 해시를 생성하여 16진수 문자열로 반환
     * 리프레시 토큰의 BCrypt 해시를 생성하기 전에 원본 토큰을 SHA-256으로 해싱하여 저장 및 비교에 사용
     */
    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new CoreException(ErrorType.INTERNAL_CRYPTO_ERROR);
        }
    }

}