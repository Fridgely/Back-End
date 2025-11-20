package soon.fridgely.domain.refrigerator.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefrigeratorUpdateRequest(

    @NotBlank(message = "냉장고 이름은 필수입니다.")
    String newName

) {
}