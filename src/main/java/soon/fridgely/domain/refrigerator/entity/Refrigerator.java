package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

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

    public static Refrigerator register(String nickname) {
        return Refrigerator.builder()
            .name(nickname == null ? DEFAULT_NAME : "%s의 냉장고".formatted(nickname))
            .invitationCode(null) // 초기에는 초대 코드 없음
            .build();
    }

    public void refreshInvitationCode(InvitationCode invitationCode) {
        this.invitationCode = invitationCode;
    }

    public void validateInvitationCode(String code, LocalDateTime now) {
        if (this.invitationCode == null) {
            throw new CoreException(ErrorType.INVALID_INVITATION_CODE); // 코드가 발급된 적 없음
        }

        if (!this.invitationCode.code().equals(code)) {
            throw new CoreException(ErrorType.INVALID_INVITATION_CODE); // 코드 불일치
        }

        if (this.invitationCode.isExpired(now)) {
            throw new CoreException(ErrorType.EXPIRED_INVITATION_CODE); // 만료됨
        }
    }

}