package soon.fridgely.domain.refrigerator.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 냉장고 초대 코드 생성기
 */
@Component
public class InvitationCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    /**
     * 랜덤 8자리 초대 코드 생성
     *
     * @return 8자리 대문자+숫자 조합 코드 (예: "ABC12345")
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

}