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

    /**
     * Handles CoreException by logging its message and data, then producing an error response.
     *
     * Logs the exception at the level specified by its ErrorType and returns a ResponseEntity
     * whose body is an ApiResponse error payload constructed from the exception's ErrorType and data.
     *
     * @param e the CoreException containing an ErrorType and optional data to include in the response
     * @return a ResponseEntity containing an ApiResponse error built from the exception's ErrorType and data
     */
    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        String logMessage = String.format("[CoreException] %s (Data: %s)",
            e.getMessage(),
            e.getData() != null ? e.getData().toString() : "null");

        ErrorType errorType = e.getErrorType();
        logByLevel(errorType, logMessage, e);

        return buildResponse(errorType, e.getData());
    }

    /**
     * Handle method argument validation failures and produce a structured error response.
     *
     * Logs each field error at warning level and returns an ApiResponse error containing a map
     * of field names to their validation messages under the INVALID_REQUEST error type.
     *
     * @param e the MethodArgumentNotValidException containing field validation errors
     * @return a ResponseEntity whose body is an ApiResponse error with ErrorType.INVALID_REQUEST and
     *         a Map<String, String> mapping field names to validation messages
     */
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

    /**
     * Handle a request missing a required servlet request parameter and produce a standardized error response.
     *
     * @param e the exception containing the name of the missing request parameter
     * @return a ResponseEntity whose body is an ApiResponse with error type MISSING_REQUEST_PARAMETER and a message identifying the missing parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        ErrorType errorType = ErrorType.MISSING_REQUEST_PARAMETER;
        String message = e.getParameterName() + "는 필수입니다.";
        log.warn("[Missing Param] {}", message);
        return buildResponse(errorType, message);
    }

    /**
     * Handles requests that use an unsupported HTTP method and produces a standardized error response.
     *
     * @param e the exception containing the unsupported method and the supported HTTP methods
     * @return a ResponseEntity whose body is an ApiResponse error with a message that names the unsupported method and lists supported methods, and whose status is ErrorType.METHOD_NOT_SUPPORTED's HTTP status
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorType errorType = ErrorType.METHOD_NOT_SUPPORTED;
        String message = String.format("'%s' 메서드는 지원하지 않습니다. (지원: %s)",
            e.getMethod(), e.getSupportedHttpMethods());
        log.warn("[Method Not Supported] {}", message);
        return buildResponse(errorType, message);
    }

    /**
     * Handle a method argument type mismatch by returning an INVALID_TYPE_VALUE API response with a human-readable message.
     *
     * @param e the exception containing the parameter name and the required type
     * @return a ResponseEntity whose body is an ApiResponse.error payload for ErrorType.INVALID_TYPE_VALUE and whose data is a message describing the expected type for the parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorType errorType = ErrorType.INVALID_TYPE_VALUE;
        Class<?> required = e.getRequiredType();
        String requiredName = required != null ? required.getSimpleName() : "알 수 없음";

        String message = String.format("'%s' 파라미터는 %s 타입이어야 합니다.", e.getName(), requiredName);
        log.warn("[Type Mismatch] {}", message);
        return buildResponse(errorType, message);
    }

    /**
     * Handle any uncaught exception and produce a standardized API error response.
     *
     * @return an ApiResponse body built for {@link ErrorType#DEFAULT_ERROR} with the HTTP status from that error type
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnknown(Exception e) {
        ErrorType errorType = ErrorType.DEFAULT_ERROR;
        log.error("[Unhandled Exception] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return buildResponse(errorType, null);
    }

    /**
     * Builds a ResponseEntity containing an error ApiResponse for the given ErrorType and data.
     *
     * @param errorType the error classification whose HTTP status and error metadata are used
     * @param data      optional additional payload to include in the error response; may be null
     * @return a ResponseEntity whose status is taken from {@code errorType} and whose body is an error ApiResponse
     */
    private ResponseEntity<ApiResponse<?>> buildResponse(ErrorType errorType, Object data) {
        return ResponseEntity
            .status(errorType.getStatus())
            .body(ApiResponse.error(errorType, data));
    }

    /**
     * Logs the provided message and optional throwable using the log level associated with the given ErrorType.
     *
     * @param errorType the ErrorType whose associated log level will be used
     * @param message   the message to log
     * @param t         an optional throwable to include with the log (if non-null it is logged alongside the message)
     */
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