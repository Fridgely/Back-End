package soon.fridgely.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.member.dto.request.DeviceTokenSyncRequest;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;
import soon.fridgely.domain.member.dto.response.MemberProfileResponse;
import soon.fridgely.domain.member.service.MemberDeviceService;
import soon.fridgely.domain.member.service.MemberFacade;
import soon.fridgely.domain.member.service.MemberService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@RestController
public class MemberController implements MemberControllerDocs {

    private final MemberFacade memberFacade;
    private final MemberService memberService;
    private final MemberDeviceService memberDeviceService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> register(
        @RequestBody @Valid MemberRegisterRequest request
    ) {
        Long memberId = memberFacade.register(request.toInfo());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberId));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(@LoginMember Long memberId) {
        MemberProfileResponse response = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PutMapping("/me/devices")
    public ResponseEntity<ApiResponse<?>> syncToken(
        @RequestBody @Valid DeviceTokenSyncRequest request,
        @LoginMember Long memberId
    ) {
        memberDeviceService.syncToken(memberId, request.token());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> updateProfileImage(
        @RequestPart("file") MultipartFile file,
        @LoginMember Long memberId
    ) {
        memberFacade.updateProfileImage(memberId, file);
        return ResponseEntity.ok(ApiResponse.success());
    }

}