package soon.fridgely.domain.refrigerator.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "냉장고 수정 요청")
public record RefrigeratorUpdateRequest(

    @Schema(description = "새로운 냉장고 이름", example = "우리집 냉장고")
    @NotBlank(message = "냉장고 이름은 필수입니다.")
    String newName

) {
}