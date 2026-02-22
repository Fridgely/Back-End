package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefrigeratorService {

    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;
    private final MemberRefrigeratorFinder memberRefrigeratorFinder;
    private final InvitationCodeGenerator codeGenerator;

    @ValidateRefrigeratorAccess(key = "#key")
    public void updateRefrigeratorName(MemberRefrigeratorKey key, RefrigeratorUpdateRequest request) {
        refrigeratorManager.update(key.refrigeratorId(), request.newName());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public InvitationCodeResponse generateInvitationCode(MemberRefrigeratorKey key) {
        String newCode = codeGenerator.generateUnique();
        LocalDateTime now = LocalDateTime.now();

        InvitationCode savedCode = refrigeratorManager.refreshInvitationCode(key.refrigeratorId(), newCode, now);
        return new InvitationCodeResponse(savedCode.getCode(), savedCode.getExpirationAt());
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

    @Transactional(readOnly = true)
    public List<RefrigeratorResponse> findAllMyRefrigerators(Long memberId) {
        return memberRefrigeratorFinder.findAllByMemberId(memberId)
            .refrigerators()
            .stream()
            .map(RefrigeratorResponse::from)
            .toList();
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public RefrigeratorResponse findRefrigerator(MemberRefrigeratorKey key) {
        MemberRefrigerator memberRefrigerator = memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId());
        return RefrigeratorResponse.from(memberRefrigerator);
    }

}