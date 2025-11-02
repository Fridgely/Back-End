package soon.fridgely.global.support.response;

import soon.fridgely.global.support.exception.ErrorMessage;
import soon.fridgely.global.support.exception.ErrorType;

public record ApiResponse<T>(
    ResultType result,
    T data,
    ErrorMessage error
) {

    /**
     * Creates a successful ApiResponse with no data or error.
     *
     * @return an ApiResponse whose result is ResultType.SUCCESS and whose data and error are null
     */
    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    /**
     * Create an ApiResponse marked as success containing the given data.
     *
     * @param data the payload to include in the successful response; may be null
     * @param <T>  type of the payload
     * @return an ApiResponse with result set to SUCCESS, the provided data, and no error
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    /**
     * Create an error ApiResponse using the provided ErrorType.
     *
     * @param error the ErrorType used to construct the response's ErrorMessage
     * @return an ApiResponse with result set to {@code ResultType.ERROR}, {@code data} null, and {@code error} set to a new {@code ErrorMessage} created from {@code error}
     */
    public static ApiResponse<?> error(ErrorType error) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error));
    }

    /**
     * Creates an API error response that includes additional error details.
     *
     * @param error the ErrorType representing the error condition
     * @param errorData supplemental contextual data for the error message
     * @return an ApiResponse with result set to ERROR, null data, and an ErrorMessage built from the provided error and errorData
     */
    public static ApiResponse<?> error(ErrorType error, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error, errorData));
    }

}