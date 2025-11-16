package soon.fridgely.global.support.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import soon.fridgely.global.support.response.ApiResponse;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiRestExceptionHandler {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        ErrorType errorType = e.getErrorType();
        Object data = e.getData();

        String logMessage = String.format("[%s] %s (Data: %s)",
            errorType.name(),
            e.getMessage(),
            data != null ? data.toString() : "null"
        );

        if (Objects.requireNonNull(errorType.getLogLevel()) == LogLevel.ERROR) {
            log.error(logMessage, e);
        } else {
            log.warn(logMessage, e);
        }

        return buildResponse(errorType, data);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        ErrorType errorType = ErrorType.INVALID_REQUEST;

        Map<String, String> validationData = e.getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효성 검사 실패"
            ));
        log.warn("[400_INVALID_REQUEST] DTO @Valid 실패. (ValidationData={})", validationData, e);

        return buildResponse(errorType, validationData);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        ErrorType errorType = ErrorType.MISSING_REQUEST_PARAMETER;
        String message = e.getParameterName() + "는 필수입니다.";
        log.warn("[400_MISSING_PARAM] 필수 파라미터 누락. (ParameterName={})", e.getParameterName(), e);

        return buildResponse(errorType, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorType errorType = ErrorType.METHOD_NOT_SUPPORTED;
        log.warn("[405_METHOD_NOT_SUPPORTED] 지원하지 않는 HTTP 메서드. (Method={}, Supported={})", e.getMethod(), e.getSupportedHttpMethods(), e);

        return buildResponse(errorType, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorType errorType = ErrorType.INVALID_TYPE_VALUE;
        String requiredName = e.getRequiredType() != null
            ? e.getRequiredType().getSimpleName()
            : "알 수 없음";
        log.warn("[400_TYPE_MISMATCH] 파라미터 타입 불일치. (Parameter={}, RequiredType={}, Value={})", e.getName(), requiredName, e.getValue(), e);

        String message = String.format("'%s' 파라미터는 %s 타입이어야 합니다.", e.getName(), requiredName);
        return buildResponse(errorType, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnknown(Exception e) {
        ErrorType errorType = ErrorType.DEFAULT_ERROR;
        log.error("[500_UNHANDLED_ERROR] 처리되지 않은 예외 발생. (Exception={}, Message={})", e.getClass().getSimpleName(), e.getMessage(), e);

        return buildResponse(errorType, null);
    }

    private ResponseEntity<ApiResponse<?>> buildResponse(ErrorType errorType, Object data) {
        return ResponseEntity
            .status(errorType.getStatus())
            .body(ApiResponse.error(errorType, data));
    }

}