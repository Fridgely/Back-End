package soon.fridgely.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import soon.fridgely.domain.member.controller.dto.request.MemberRegisterRequest;
import soon.fridgely.domain.member.service.MemberService;
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

}