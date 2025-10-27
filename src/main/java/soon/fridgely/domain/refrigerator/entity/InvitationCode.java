package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Embeddable
public record InvitationCode(

    @Column(name = "invitation_code", length = 8, nullable = false)
    String code,

    @Column(nullable = false)
    LocalDateTime expirationAt

) {

    private static final long DEFAULT_EXPIRATION_DAYS = 1L;

    public static InvitationCode generate(String invitationCode, LocalDateTime now) {
        return new InvitationCode(requireNonNull(invitationCode, "초대 코드는 필수입니다."), now.plusDays(DEFAULT_EXPIRATION_DAYS));
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expirationAt);
    }

}