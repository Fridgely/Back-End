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
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.", LogLevel.WARN),
    AUTHORIZATION_FAILED(HttpStatus.FORBIDDEN, "권한이 없습니다.", LogLevel.WARN),

    // 스토리지 오류
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.", LogLevel.ERROR),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제를 실패했습니다.", LogLevel.ERROR),
    STORAGE_PRESIGNED_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Presigned URL 생성에 실패했습니다.", LogLevel.ERROR),

    // 멤버 오류
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 ID입니다.", LogLevel.WARN),

    // 냉장고 오류
    INVALID_REFRIGERATOR_ACCESS_KEY(HttpStatus.BAD_REQUEST, "유효하지 않은 냉장고 접근 키입니다.", LogLevel.INFO),

    // 카테고리 오류
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다.", LogLevel.WARN),
    CANNOT_MODIFY_DEFAULT_CATEGORY(HttpStatus.BAD_REQUEST, "기본 카테고리는 수정할 수 없습니다.", LogLevel.INFO),
    ;

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;

}