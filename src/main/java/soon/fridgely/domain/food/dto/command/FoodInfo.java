package soon.fridgely.domain.food.dto.command;

import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.time.LocalDate;

public record FoodInfo(
    String name,
    Quantity quantity,
    FoodCondition condition,
    String description,
    String imageURL
) {

    public Food toEntity(Member member, Refrigerator refrigerator, Category category, LocalDate now) {
        return Food.register(
            refrigerator,
            member,
            name,
            category,
            quantity,
            condition.expirationDate(),
            condition.storageType(),
            description,
            imageURL,
            now
        );
    }

}