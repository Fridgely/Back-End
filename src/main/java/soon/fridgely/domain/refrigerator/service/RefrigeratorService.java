package soon.fridgely.domain.refrigerator.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorMemberResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefrigeratorService {

    private final RefrigeratorRepository refrigeratorRepository;
    private final MemberRefrigeratorRepository memberRefrigeratorRepository;
    private final MemberRepository memberRepository;
    private final InvitationCodeGenerator codeGenerator;

    @ValidateRefrigeratorAccess(key = "#key")
    @CacheEvict(value = "myRefrigerators", allEntries = true)
    @Transactional
    public void updateRefrigeratorName(MemberRefrigeratorKey key, RefrigeratorUpdateRequest request) {
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        refrigerator.update(request.newName());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Retry(name = "invitationCodeGeneration", fallbackMethod = "generateInvitationCodeFallback")
    @Transactional // ObjectOptimisticLockingFailureException은 커밋 시점에 발생 — 재시도마다 새 트랜잭션이 필요
    public InvitationCodeResponse generateInvitationCode(MemberRefrigeratorKey key) {
        String newCode = codeGenerator.generateUnique();
        LocalDateTime now = LocalDateTime.now();
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        InvitationCode invitationCode = InvitationCode.generate(newCode, now);
        refrigerator.refreshInvitationCode(invitationCode);
        return new InvitationCodeResponse(invitationCode.getCode(), invitationCode.getExpirationAt());
    }

    @CacheEvict(value = "myRefrigerators", key = "#memberId")
    @Transactional
    public void joinByInvitationCode(Long memberId, String code) {
        Refrigerator refrigerator = refrigeratorRepository.findByInvitationCode_code(code)
            .filter(r -> r.getStatus() == EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.INVALID_INVITATION_CODE));
        refrigerator.validateInvitationCode(code, LocalDateTime.now());

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));

        memberRefrigeratorRepository.findByMemberIdAndRefrigeratorIdIgnoreStatus(member.getId(), refrigerator.getId())
            .ifPresentOrElse(
                existing -> {
                    if (existing.isActive()) {
                        throw new CoreException(ErrorType.ALREADY_JOINED_REFRIGERATOR);
                    }
                    existing.active();
                },
                () -> {
                    try {
                        memberRefrigeratorRepository.save(
                            MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER));
                    } catch (DataIntegrityViolationException e) {
                        throw new CoreException(ErrorType.ALREADY_JOINED_REFRIGERATOR);
                    }
                }
            );
    }

    @Cacheable(value = "myRefrigerators", key = "#memberId")
    @Transactional(readOnly = true)
    public List<RefrigeratorResponse> findAllMyRefrigerators(Long memberId) {
        return memberRefrigeratorRepository.findAllMyRefrigerators(memberId, EntityStatus.ACTIVE)
            .stream()
            .map(RefrigeratorResponse::from)
            .collect(Collectors.toList());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public RefrigeratorResponse findRefrigerator(MemberRefrigeratorKey key) {
        MemberRefrigerator memberRefrigerator = memberRefrigeratorRepository
            .findByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        return RefrigeratorResponse.from(memberRefrigerator);
    }

    @CacheEvict(value = "myRefrigerators", key = "#key.memberId()")
    @Transactional
    public void leaveRefrigerator(MemberRefrigeratorKey key) {
        Optional<MemberRefrigerator> memberRefrigerator = memberRefrigeratorRepository
            .findByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId(), EntityStatus.ACTIVE);
        if (memberRefrigerator.isEmpty()) {
            return;
        }
        memberRefrigerator.get().validateCanLeave();
        memberRefrigerator.get().delete();
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public List<RefrigeratorMemberResponse> findAllMembers(MemberRefrigeratorKey key) {
        return memberRefrigeratorRepository.findAllByRefrigeratorId(key.refrigeratorId(), EntityStatus.ACTIVE)
            .stream()
            .map(RefrigeratorMemberResponse::from)
            .toList();
    }

    @Transactional
    public Refrigerator register(Member member) {
        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        return refrigeratorRepository.saveAndFlush(refrigerator);
    }

    @CacheEvict(value = "myRefrigerators", key = "#member.id")
    @Transactional
    public void linkToOwner(Member member, Refrigerator refrigerator) {
        memberRefrigeratorRepository.save(MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER));
    }

    @Transactional(readOnly = true)
    public MemberRefrigerator findMemberRefrigerator(long memberId, long refrigeratorId) {
        return memberRefrigeratorRepository.findByMemberIdAndRefrigeratorId(memberId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @CacheEvict(value = "myRefrigerators", allEntries = true)
    @Transactional
    public void unlinkAll(long refrigeratorId) {
        List<MemberRefrigerator> links = memberRefrigeratorRepository.findAllByRefrigeratorId(refrigeratorId, EntityStatus.ACTIVE);
        links.forEach(MemberRefrigerator::delete);
    }

    @CacheEvict(value = "myRefrigerators", allEntries = true)
    @Transactional
    public void delete(long refrigeratorId) {
        Refrigerator refrigerator = refrigeratorRepository.findById(refrigeratorId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        if (refrigerator.isDeleted()) {
            return;
        }
        refrigerator.delete();
    }

    InvitationCodeResponse generateInvitationCodeFallback(MemberRefrigeratorKey key, Exception e) {
        log.warn("[Refrigerator] 초대 코드 생성 재시도 횟수 초과. (RefrigeratorId={})", key.refrigeratorId(), e);
        throw new CoreException(ErrorType.CONCURRENT_UPDATE_LIMIT_EXCEEDED);
    }
}
