package soon.fridgely.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    public static MemberDevice register(Member member, String token) {
        return MemberDevice.builder()
            .member(member)
            .token(token)
            .lastUsedAt(LocalDateTime.now())
            .build();
    }

    public void updateToken(String newToken) {
        this.token = newToken;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void refreshLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

}