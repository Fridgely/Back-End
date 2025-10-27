package soon.fridgely.domain.food.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.food.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
}