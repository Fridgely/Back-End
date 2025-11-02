package soon.fridgely.global.support.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    // 공통 오류
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다.", LogLevel.INFO),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, "해당 데이터를 찾을 수 없습니다.", LogLevel.ERROR),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다.", LogLevel.INFO),
    METHOD_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.", LogLevel.INFO),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다.", LogLevel.INFO),

    // 멤버 오류
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 ID입니다.", LogLevel.WARN),

    ;

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;

}