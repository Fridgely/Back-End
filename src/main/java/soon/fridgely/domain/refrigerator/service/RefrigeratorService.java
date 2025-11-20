package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class RefrigeratorService {

    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;
    private final InvitationCodeGenerator codeGenerator;

    @ValidateRefrigeratorAccess(key = "#key")
    public InvitationCodeResponse generateInvitationCode(MemberRefrigeratorKey key) {
        String newCode = codeGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        InvitationCode savedCode = refrigeratorManager.refreshInvitationCode(key.refrigeratorId(), newCode, now);
        return new InvitationCodeResponse(savedCode.code(), savedCode.expirationAt());
    }

    @Transactional
    public void joinByInvitationCode(Long memberId, String code) {
        Refrigerator refrigerator = refrigeratorManager.findByInvitationCode(code);
        refrigerator.validateInvitationCode(code, LocalDateTime.now());

        memberRefrigeratorLinker.linkMemberToRefrigerator(
            new MemberRefrigeratorKey(memberId, refrigerator.getId()),
            RefrigeratorRole.MEMBER
        );
    }

}