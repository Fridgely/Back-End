package soon.fridgely.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.category.dto.command.AddCategory;

public record CategoryAddRequest(

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    String name

) {

    public AddCategory toAddCategory(long refrigeratorId, long memberId) {
        return new AddCategory(name, refrigeratorId, memberId);
    }

}