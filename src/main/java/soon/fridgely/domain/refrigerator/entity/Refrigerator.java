package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "refrigerators",
    uniqueConstraints = @UniqueConstraint(name = "uk_refrigerators_invitation_code", columnNames = "invitation_code")
)
@Entity
public class Refrigerator extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Embedded
    private InvitationCode invitationCode;

    private static final String DEFAULT_NAME = "무제";

    public static Refrigerator register(String name) {
        return Refrigerator.builder()
            .name(name == null ? DEFAULT_NAME : name)
            .invitationCode(null) // 초기에는 초대 코드 없음
            .build();
    }

    public void refreshInvitationCode(InvitationCode invitationCode) {
        this.invitationCode = invitationCode;
    }

}