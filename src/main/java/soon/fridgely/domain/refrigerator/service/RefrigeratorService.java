package soon.fridgely.domain.refrigerator.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.service.CategoryRemover;
import soon.fridgely.domain.food.service.FoodRemover;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorMemberResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefrigeratorService {

    private final RefrigeratorManager refrigeratorManager;
    private final MemberRefrigeratorLinker memberRefrigeratorLinker;
    private final MemberRefrigeratorFinder memberRefrigeratorFinder;
    private final InvitationCodeGenerator codeGenerator;
    private final FoodRemover foodRemover;
    private final CategoryRemover categoryRemover;

    @ValidateRefrigeratorAccess(key = "#key")
    public void updateRefrigeratorName(MemberRefrigeratorKey key, RefrigeratorUpdateRequest request) {
        refrigeratorManager.update(key.refrigeratorId(), request.newName());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Retry(name = "invitationCodeGeneration", fallbackMethod = "generateInvitationCodeFallback")
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

    @Transactional
    public void leaveRefrigerator(MemberRefrigeratorKey key) {
        Optional<MemberRefrigerator> memberRefrigerator = memberRefrigeratorFinder.findOptionalByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId());
        if (memberRefrigerator.isEmpty()) {
            return;
        }
        if (memberRefrigerator.get().isOwner()) { // TODO: 냉장고 삭제 기능 구현 후 MEMBER가 존재하지 않는다면 냉장고도 삭제하도록 변경
            throw new CoreException(ErrorType.OWNER_CANNOT_LEAVE_REFRIGERATOR);
        }
        memberRefrigeratorLinker.unlink(key);
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void deleteRefrigerator(MemberRefrigeratorKey key) {
        MemberRefrigerator memberRefrigerator = memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId());
        if (!memberRefrigerator.isOwner()) {
            throw new CoreException(ErrorType.ONLY_OWNER_CAN_DELETE_REFRIGERATOR);
        }
        foodRemover.removeAllByRefrigeratorId(key.refrigeratorId());
        categoryRemover.removeAllByRefrigeratorId(key.refrigeratorId());
        memberRefrigeratorLinker.unlinkAll(key.refrigeratorId());
        refrigeratorManager.delete(key.refrigeratorId());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public List<RefrigeratorMemberResponse> findAllMembers(MemberRefrigeratorKey key) {
        return memberRefrigeratorFinder.findAllMembersByRefrigeratorId(key.refrigeratorId())
            .stream()
            .map(RefrigeratorMemberResponse::from)
            .toList();
    }

    private InvitationCodeResponse generateInvitationCodeFallback(MemberRefrigeratorKey key, Exception e) {
        log.warn("초대 코드 생성 재시도 횟수 초과 - refrigeratorId: {}", key.refrigeratorId(), e);
        throw new CoreException(ErrorType.CONCURRENT_UPDATE_LIMIT_EXCEEDED);
    }

}