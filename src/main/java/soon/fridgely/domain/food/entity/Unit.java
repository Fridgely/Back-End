package soon.fridgely.domain.food.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Unit {

    PIECE("개"),
    ML("ml"),
    L("L"),
    G("g"),
    KG("kg");

    private final String label;

}