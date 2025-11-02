package soon.fridgely.global.support.exception;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final ErrorType errorType;
    private final Object data;

    /**
     * Creates a CoreException associated with the given error type.
     *
     * The exception message is set from {@code errorType.getMessage()} and the
     * data payload is initialized to {@code null}.
     *
     * @param errorType the error type that categorizes this exception
     */
    public CoreException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
    }

    /**
     * Creates a CoreException associated with the given error type and optional contextual data.
     *
     * The exception message is set from the provided ErrorType's message.
     *
     * @param errorType the ErrorType describing the error condition
     * @param data optional contextual data related to the error; may be null
     */
    public CoreException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

}