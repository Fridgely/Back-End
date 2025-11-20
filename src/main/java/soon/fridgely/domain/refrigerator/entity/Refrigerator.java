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

    @Version
    private Long version;

    private static final String DEFAULT_NAME = "무제";

    public static Refrigerator register(String nickname) {
        return Refrigerator.builder()
            .name(nickname == null ? DEFAULT_NAME : "%s의 냉장고".formatted(nickname))
            .invitationCode(null) // 초기에는 초대 코드 없음
            .build();
    }

    public void update(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "냉장고 이름은 비어 있을 수 없습니다.");
        }

        if (!this.name.equals(name)) {
            this.name = name;
        }
    }

    public void refreshInvitationCode(InvitationCode invitationCode) {
        this.invitationCode = invitationCode;
    }

    public void validateInvitationCode(String code, LocalDateTime now) {
        if (this.invitationCode == null) {
            throw new CoreException(ErrorType.INVALID_INVITATION_CODE);
        }

        this.invitationCode.validate(code, now);
    }

}