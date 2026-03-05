package soon.fridgely.domain.food.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FoodSortType {

    EXPIRATION("유통기한 임박순"),
    CREATED("등록순"),
    NAME("이름순");

    private final String description;

}