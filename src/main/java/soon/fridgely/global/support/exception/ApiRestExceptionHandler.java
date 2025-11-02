package soon.fridgely.global.support.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import soon.fridgely.global.support.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiRestExceptionHandler {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        String logMessage = String.format("[CoreException] %s (Data: %s)",
            e.getMessage(),
            e.getData() != null ? e.getData().toString() : "null");

        ErrorType errorType = e.getErrorType();
        logByLevel(errorType, logMessage, e);

        return buildResponse(errorType, e.getData());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        ErrorType errorType = ErrorType.INVALID_REQUEST;
        Map<String, String> validation = new HashMap<>();
        e.getFieldErrors().forEach(fe -> {
            validation.put(fe.getField(), fe.getDefaultMessage());
            log.warn("[Validation Error] Field: {}, Message: {}", fe.getField(), fe.getDefaultMessage());
        });
        return buildResponse(errorType, validation);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        ErrorType errorType = ErrorType.MISSING_REQUEST_PARAMETER;
        String message = e.getParameterName() + "는 필수입니다.";
        log.warn("[Missing Param] {}", message);
        return buildResponse(errorType, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorType errorType = ErrorType.METHOD_NOT_SUPPORTED;
        String message = String.format("'%s' 메서드는 지원하지 않습니다. (지원: %s)",
            e.getMethod(), e.getSupportedHttpMethods());
        log.warn("[Method Not Supported] {}", message);
        return buildResponse(errorType, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorType errorType = ErrorType.INVALID_TYPE_VALUE;
        Class<?> required = e.getRequiredType();
        String requiredName = required != null ? required.getSimpleName() : "알 수 없음";

        String message = String.format("'%s' 파라미터는 %s 타입이어야 합니다.", e.getName(), requiredName);
        log.warn("[Type Mismatch] {}", message);
        return buildResponse(errorType, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnknown(Exception e) {
        ErrorType errorType = ErrorType.DEFAULT_ERROR;
        log.error("[Unhandled Exception] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return buildResponse(errorType, null);
    }

    private ResponseEntity<ApiResponse<?>> buildResponse(ErrorType errorType, Object data) {
        return ResponseEntity
            .status(errorType.getStatus())
            .body(ApiResponse.error(errorType, data));
    }

    private void logByLevel(ErrorType errorType, String message, Throwable t) {
        switch (errorType.getLogLevel()) {
            case ERROR -> {
                if (t != null) log.error(message, t);
                else log.error(message);
            }
            case WARN -> log.warn(message);
            case INFO -> log.info(message);
            case DEBUG -> log.debug(message);
            default -> log.trace(message);
        }
    }

}