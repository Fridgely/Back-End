package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationCode {

    private static final long DEFAULT_EXPIRATION_DAYS = 1L;

    @Column(name = "invitation_code", length = 8)
    private String code;

    @Column
    private LocalDateTime expirationAt;

    private InvitationCode(String code, LocalDateTime expirationAt) {
        requireNonNull(code, "초대 코드는 필수입니다.");
        requireNonNull(expirationAt, "만료 시간은 필수입니다.");
        this.code = code;
        this.expirationAt = expirationAt;
    }

    public static InvitationCode generate(String invitationCode, LocalDateTime now) {
        requireNonNull(now, "기준 시간은 필수입니다.");
        return new InvitationCode(invitationCode, now.plusDays(DEFAULT_EXPIRATION_DAYS));
    }

    public void validate(String inputCode, LocalDateTime now) {
        if (!this.code.equals(inputCode)) {
            throw new CoreException(ErrorType.INVALID_INVITATION_CODE);
        }

        if (isExpired(now)) {
            throw new CoreException(ErrorType.EXPIRED_INVITATION_CODE);
        }
    }

    private boolean isExpired(LocalDateTime now) {
        return requireNonNull(now, "현재 시간은 필수입니다.").isAfter(expirationAt);
    }

}