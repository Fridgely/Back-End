package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.service.FoodStockHandler;

@RequiredArgsConstructor
@Component
public class StockExhaustionNotificationHandler implements FoodStockHandler {

    private final NotificationProcessor notificationProcessor;

    @Override
    public void onStockExhausted(Food food) {
        notificationProcessor.processStockExhaustion(food);
    }

}