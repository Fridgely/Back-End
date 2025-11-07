package soon.fridgely.domain.category.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import soon.fridgely.domain.category.dto.NewCategory;

public record CategoryAddRequest(

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    String name

) {

    public NewCategory toNewCategory(long memberId, long refrigeratorId) {
        return new NewCategory(name, refrigeratorId, memberId);
    }

}