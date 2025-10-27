package soon.fridgely.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import soon.fridgely.domain.BaseEntity;

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

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRole() {
        return role.name();
    }

}