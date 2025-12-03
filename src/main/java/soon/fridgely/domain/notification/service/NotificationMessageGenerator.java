package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.notification.dto.command.NotificationMessage;

import java.util.List;

@RequiredArgsConstructor
@Component
public class NotificationMessageGenerator {

    private static final String NOTIFICATION_TITLE = "유통기한 임박 알림 ⏰";

    public NotificationMessage generate(List<Food> foods, int days) {
        int count = foods.size();
        String foodName = foods.get(0).getName();
        String body = (count == 1)
            ? "'%s'의 소비기한이 %d일 남았습니다.".formatted(foodName, days)
            : "'%s' 외 %d개 품목의 소비기한이 %d일 남았습니다.".formatted(foodName, count - 1, days);

        return new NotificationMessage(NOTIFICATION_TITLE, body);
    }

}