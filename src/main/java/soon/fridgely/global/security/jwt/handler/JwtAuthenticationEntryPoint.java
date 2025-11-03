package soon.fridgely.global.security.jwt.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.SecurityHandlerHelper;

import java.io.IOException;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근 시 401 Unauthorized 에러를 처리하는 핸들러
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityHandlerHelper handlerHelper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        log.warn("인증 실패: {}, URI: {}", authException.getMessage(), request.getRequestURI());
        handlerHelper.sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
    }

}