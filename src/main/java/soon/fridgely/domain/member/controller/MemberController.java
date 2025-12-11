package soon.fridgely.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soon.fridgely.domain.member.dto.request.DeviceTokenSyncRequest;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;
import soon.fridgely.domain.member.service.MemberService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@RestController
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> register(
        @RequestBody @Valid MemberRegisterRequest request
    ) {
        Long memberId = memberService.register(request.toInfo());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberId));
    }

    @PutMapping("/me/devices")
    public ResponseEntity<ApiResponse<?>> syncToken(
        @RequestBody @Valid DeviceTokenSyncRequest request,
        @LoginMember Long memberId
    ) {
        memberService.syncToken(memberId, request.token());
        return ResponseEntity.ok(ApiResponse.success());
    }

}