package soon.fridgely.domain.food.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StorageType {

    FROZEN("냉동"),
    REFRIGERATION("냉장"),
    ROOM_TEMPERATURE("상온");

    private final String description;

}