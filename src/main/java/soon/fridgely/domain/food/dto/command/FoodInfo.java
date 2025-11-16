package soon.fridgely.domain.food.dto.command;

import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.time.LocalDateTime;

public record FoodInfo(
    String name,
    long categoryId,
    Quantity quantity,
    LocalDateTime expirationDate,
    StorageType storageType,
    FoodStatus foodStatus,
    String description,
    String imageURL
) {

    public Food toEntity(Member member, Refrigerator refrigerator, Category category) {
        return Food.register(
            refrigerator,
            member,
            name,
            category,
            quantity,
            expirationDate,
            storageType,
            foodStatus,
            description,
            imageURL
        );
    }

}