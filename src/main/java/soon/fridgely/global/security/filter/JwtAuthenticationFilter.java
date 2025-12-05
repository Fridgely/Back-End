package soon.fridgely.global.security.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import soon.fridgely.domain.auth.provider.TokenProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.SecurityHandlerHelper;

import java.io.IOException;
import java.util.Optional;

/**
 * HTTP 요청 헤더의 Authorization에서 JWT를 추출하여,
 * 토큰이 유효하면 SecurityContext에 인증 정보를 저장하는 필터
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final SecurityHandlerHelper handlerHelper;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> tokenOpt = resolveToken(request);
        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = tokenOpt.get();
        try {
            if (!tokenProvider.validateToken(token)) {
                handlerHelper.sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
                return;
            }

            Authentication auth = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (CoreException | JwtException e) {
            log.warn("JWT 인증 처리 중 예외 발생: {}", e.getMessage());
            handlerHelper.sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED);
            return;
        }

        filterChain.doFilter(request, response);

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/reissue");
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        return (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX))
            ? Optional.of(bearer.substring(BEARER_PREFIX.length()))
            : Optional.empty();
    }

}