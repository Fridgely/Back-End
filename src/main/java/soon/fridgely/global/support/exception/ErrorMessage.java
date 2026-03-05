package soon.fridgely.global.support.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 정보")
public record ErrorMessage(

    @Schema(description = "HTTP 상태 코드명", example = "BAD_REQUEST")
    String code,

    @Schema(description = "에러 메시지", example = "요청이 올바르지 않습니다.")
    String message,

    @Schema(description = "추가 에러 데이터")
    Object data

) {

    public ErrorMessage(ErrorType errorType) {
        this(errorType.getStatus().name(), errorType.getMessage(), null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getStatus().name(), errorType.getMessage(), data);
    }

}