package soon.fridgely.global.support.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.IOException;

/**
 * Spring Security의 EntryPoint 및 Handler에서
 * 공통적으로 사용되는 JSON 에러 응답 로직을 처리하는 헬퍼
 */
@RequiredArgsConstructor
@Component
public class SecurityHandlerHelper {

    private final ObjectMapper objectMapper;

    public void sendErrorResponse(
        HttpServletResponse response,
        ErrorType errorType
    ) throws IOException {
        String json = objectMapper.writeValueAsString(ApiResponse.error(errorType));

        response.setStatus(errorType.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

}