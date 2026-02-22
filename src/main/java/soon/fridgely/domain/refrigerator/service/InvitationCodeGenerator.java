package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.security.SecureRandom;

/**
 * 냉장고 초대 코드 생성
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class InvitationCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final RefrigeratorRepository refrigeratorRepository;

    /**
     * 고유한 초대 코드 생성
     */
    public String generateUnique() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = generate();

            if (!refrigeratorRepository.existsByInvitationCode_codeAndStatus(code, EntityStatus.ACTIVE)) {
                if (attempt > 1) {
                    log.debug("[InvitationCode] 중복 체크 후 생성 성공. (Attempt={})", attempt);
                }
                return code;
            }

            log.debug("[InvitationCode] 중복 발생, 재생성. (Attempt={})", attempt);
        }

        log.error("[InvitationCode] {}회 재시도 후에도 고유 코드 생성 실패.", MAX_GENERATION_ATTEMPTS);
        throw new CoreException(ErrorType.INVITATION_CODE_GENERATION_FAILED);
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

}