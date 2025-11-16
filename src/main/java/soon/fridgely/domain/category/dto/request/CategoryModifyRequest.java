package soon.fridgely.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.category.dto.command.ModifyCategory;

public record CategoryModifyRequest(

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