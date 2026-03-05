package soon.fridgely.domain.category.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.category.dto.command.ModifyCategory;

@Schema(description = "카테고리 수정 요청")
public record CategoryModifyRequest(

    @Schema(description = "새로운 카테고리 이름", example = "음료류")
    @NotBlank(message = "새로운 카테고리 이름은 필수입니다.")
    String newName

) {

    public ModifyCategory toModifyCategory(
        long memberId,
        long refrigeratorId,
        long categoryId
    ) {
        return new ModifyCategory(newName, memberId, refrigeratorId, categoryId);
    }

}