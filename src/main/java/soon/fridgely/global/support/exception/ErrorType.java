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
    CONCURRENT_UPDATE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "동시 요청이 많아 처리에 실패했습니다. 잠시 후 다시 시도해주세요.", LogLevel.WARN),

    // 스토리지 오류
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.", LogLevel.ERROR),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제를 실패했습니다.", LogLevel.ERROR),
    STORAGE_PRESIGNED_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Presigned URL 생성에 실패했습니다.", LogLevel.ERROR),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 허용 범위를 초과했습니다. (최대 10MB)", LogLevel.WARN),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다.", LogLevel.WARN),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 URL입니다.", LogLevel.WARN),

    // 멤버 오류
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 ID입니다.", LogLevel.WARN),

    // 냉장고 오류
    INVALID_REFRIGERATOR_ACCESS_KEY(HttpStatus.BAD_REQUEST, "유효하지 않은 냉장고 접근 키입니다.", LogLevel.INFO),
    ALREADY_JOINED_REFRIGERATOR(HttpStatus.BAD_REQUEST, "이미 가입된 냉장고입니다.", LogLevel.INFO),
    OWNER_CANNOT_LEAVE_REFRIGERATOR(HttpStatus.BAD_REQUEST, "냉장고 소유자는 냉장고를 나갈 수 없습니다.", LogLevel.INFO),
    ONLY_OWNER_CAN_DELETE_REFRIGERATOR(HttpStatus.FORBIDDEN, "냉장고 소유자만 냉장고를 삭제할 수 있습니다.", LogLevel.WARN),

    // 초대 코드 오류
    INVALID_INVITATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다.", LogLevel.INFO),
    EXPIRED_INVITATION_CODE(HttpStatus.BAD_REQUEST, "만료된 초대 코드입니다.", LogLevel.INFO),
    INVITATION_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    // 카테고리 오류
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다.", LogLevel.WARN),
    CANNOT_MODIFY_DEFAULT_CATEGORY(HttpStatus.BAD_REQUEST, "기본 카테고리는 수정할 수 없습니다.", LogLevel.INFO),

    // 알림 오류
    EMPTY_NOTIFICATION_TARGET(HttpStatus.INTERNAL_SERVER_ERROR, "알림 메시지를 생성할 대상 데이터가 없습니다.", LogLevel.ERROR),
    FIREBASE_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Firebase 초기화에 실패했습니다.", LogLevel.ERROR),
    ;

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;

}