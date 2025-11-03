package soon.fridgely.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import soon.fridgely.domain.auth.controller.dto.request.LoginRequest;
import soon.fridgely.domain.auth.controller.dto.request.ReissueTokenRequest;
import soon.fridgely.domain.auth.service.AuthService;
import soon.fridgely.global.security.jwt.dto.response.TokenResponse;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
        @RequestBody @Valid LoginRequest request
    ) {
        TokenResponse response = authService.login(request.toLoginInfo());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
        @RequestBody @Valid ReissueTokenRequest request
    ) {
        TokenResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}