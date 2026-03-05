package soon.fridgely.domain.category.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.category.dto.command.AddCategory;

@Schema(description = "카테고리 추가 요청")
public record CategoryAddRequest(

    @Schema(description = "카테고리 이름", example = "간식류")
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    String name

) {

    public AddCategory toAddCategory(long refrigeratorId, long memberId) {
        return new AddCategory(name, refrigeratorId, memberId);
    }

}