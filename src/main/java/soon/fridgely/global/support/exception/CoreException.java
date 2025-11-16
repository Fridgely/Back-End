package soon.fridgely.global.support.exception;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final ErrorType errorType;
    private final Object data;
    private final String detailMessage;

    public CoreException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
        this.detailMessage = null;
    }

    public CoreException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
        this.detailMessage = null;
    }

    public CoreException(ErrorType errorType, String detailMessage) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
        this.detailMessage = detailMessage;
    }

    public CoreException(ErrorType errorType, Object data, String detailMessage) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
        this.detailMessage = detailMessage;
    }

    @Override
    public String getMessage() {
        if (detailMessage == null) {
            return super.getMessage();
        }
        return String.format("%s (Detail: %s)", super.getMessage(), detailMessage);
    }

}