package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Component
public class FoodRemover {

    private final FoodRepository foodRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void remove(long foodId, long refrigeratorId) {
        Food food = foodRepository.findByIdAndRefrigeratorId(foodId, refrigeratorId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        if (food.isDeleted()) {
            return;
        }
        publishImageDeleteEventIfPresent(food);
        food.delete();
    }

    @Transactional
    public void removeAllByRefrigeratorId(long refrigeratorId) {
        List<Food> foods = foodRepository.findAllByRefrigeratorIdAndStatus(refrigeratorId, EntityStatus.ACTIVE);
        for (Food food : foods) {
            publishImageDeleteEventIfPresent(food);
            food.delete();
        }
    }

    private void publishImageDeleteEventIfPresent(Food food) {
        String imageUrl = food.getImageURL();
        if (StringUtils.hasText(imageUrl)) {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageUrl));
        }
    }

}
