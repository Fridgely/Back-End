package soon.fridgely.domain.refrigerator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.InvitationCodeJoinRequest;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorMemberResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.service.RefrigeratorService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/refrigerators")
@RestController
public class RefrigeratorController implements RefrigeratorControllerDocs {

    private final RefrigeratorService refrigeratorService;

    @Override
    @GetMapping("/{refrigeratorId}")
    public ResponseEntity<ApiResponse<RefrigeratorResponse>> findRefrigerator(
        @LoginMember Long memberId,
        @PathVariable Long refrigeratorId
    ) {
        RefrigeratorResponse response = refrigeratorService.findRefrigerator(new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<RefrigeratorResponse>>> findAllMyRefrigerators(
        @LoginMember Long memberId
    ) {
        List<RefrigeratorResponse> responses = refrigeratorService.findAllMyRefrigerators(memberId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Override
    @PatchMapping("/{refrigeratorId}")
    public ResponseEntity<ApiResponse<?>> updateRefrigerator(
        @RequestBody @Valid RefrigeratorUpdateRequest request,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        refrigeratorService.updateRefrigeratorName(new MemberRefrigeratorKey(memberId, refrigeratorId), request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/{refrigeratorId}/invitation-codes")
    public ResponseEntity<ApiResponse<InvitationCodeResponse>> generateInvitationCode(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        InvitationCodeResponse response = refrigeratorService.generateInvitationCode(new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PostMapping("/invitation-codes/join")
    public ResponseEntity<ApiResponse<?>> joinByInvitationCode(
        @RequestBody @Valid InvitationCodeJoinRequest request,
        @LoginMember Long memberId
    ) {
        refrigeratorService.joinByInvitationCode(memberId, request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @GetMapping("/{refrigeratorId}/members")
    public ResponseEntity<ApiResponse<List<RefrigeratorMemberResponse>>> findAllMembers(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        List<RefrigeratorMemberResponse> responses = refrigeratorService.findAllMembers(new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

}