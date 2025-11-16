package soon.fridgely.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.SecurityHandlerHelper;

import java.io.IOException;

/**
 * 인증은 되었으나, 필요한 권한이 없어 리소스 접근에 실패했을 때 403 Forbidden 에러를 처리하는 핸들러
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityHandlerHelper handlerHelper;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("접근 권한 없음: {}, URI: {}", accessDeniedException.getMessage(), request.getRequestURI());
        handlerHelper.sendErrorResponse(response, ErrorType.AUTHORIZATION_FAILED);
    }

}