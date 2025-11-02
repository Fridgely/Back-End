package soon.fridgely.global.support.exception;

public record ErrorMessage(
    String code,
    String message,
    Object data
) {

    /**
     * Creates an ErrorMessage from an ErrorType, using the error type's status name as the code
     * and the error type's message; `data` is set to {@code null}.
     *
     * @param errorType the source ErrorType from which to derive the code and message
     */
    public ErrorMessage(ErrorType errorType) {
        this(errorType.getStatus().name(), errorType.getMessage(), null);
    }

    /**
     * Create an ErrorMessage using values from the provided ErrorType and an explicit data payload.
     *
     * @param errorType the ErrorType whose status name and message populate this record's `code` and `message` fields
     * @param data      additional information to include in the `data` field (may be null)
     */
    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getStatus().name(), errorType.getMessage(), data);
    }

}