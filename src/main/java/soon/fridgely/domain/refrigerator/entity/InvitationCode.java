package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Embeddable
public record InvitationCode(

    @Column(name = "invitation_code", length = 8)
    String code,

    @Column
    LocalDateTime expirationAt

) {

    public InvitationCode {
        requireNonNull(code, "초대 코드는 필수입니다.");
        requireNonNull(expirationAt, "만료 시간은 필수입니다.");
    }

    private static final long DEFAULT_EXPIRATION_DAYS = 1L;

    public static InvitationCode generate(String invitationCode, LocalDateTime now) {
        requireNonNull(now, "기준 시간은 필수입니다.");
        return new InvitationCode(invitationCode, now.plusDays(DEFAULT_EXPIRATION_DAYS));
    }

    public boolean isExpired(LocalDateTime now) {
        return requireNonNull(now, "현재 시간은 필수입니다.").isAfter(expirationAt);
    }

}