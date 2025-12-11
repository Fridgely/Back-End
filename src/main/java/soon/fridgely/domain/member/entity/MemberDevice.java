package soon.fridgely.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "member_devices",
    uniqueConstraints = @UniqueConstraint(name = "uk_member_device_token", columnNames = "token")
)
@Entity
public class MemberDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    public static MemberDevice register(Member member, String token, LocalDateTime now) {
        return MemberDevice.builder()
            .member(member)
            .token(requireNonNull(token, "token은 필수입니다."))
            .lastUsedAt(requireNonNull(now, "now는 필수입니다."))
            .build();
    }

    public void updateToken(String newToken, LocalDateTime now) {
        this.token = newToken;
        refreshLastUsedAt(now);
    }

    public void refreshLastUsedAt(LocalDateTime now) {
        this.lastUsedAt = now;
    }

}