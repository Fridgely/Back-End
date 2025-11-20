package soon.fridgely.domain.refrigerator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.InvitationCodeJoinRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.service.RefrigeratorService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/refrigerators")
@RestController
public class RefrigeratorController {

    private final RefrigeratorService refrigeratorService;

    @PostMapping("/{refrigeratorId}/invitation-codes")
    public ResponseEntity<ApiResponse<InvitationCodeResponse>> generateInvitationCode(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        InvitationCodeResponse response = refrigeratorService.generateInvitationCode(new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/invitation-codes/join")
    public ResponseEntity<ApiResponse<?>> joinByInvitationCode(
        @RequestBody @Valid InvitationCodeJoinRequest request,
        @LoginMember Long memberId
    ) {
        refrigeratorService.joinByInvitationCode(memberId, request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

}