package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.service.FoodStockHandler;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockExhaustionNotificationHandler implements FoodStockHandler {

    @Override
    public void onStockExhausted(Food food) {
        throw new UnsupportedOperationException("Unsupported onStockExhausted");
    }

}