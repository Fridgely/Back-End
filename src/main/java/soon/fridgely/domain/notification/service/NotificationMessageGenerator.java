package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.notification.dto.command.NotificationMessage;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.List;

@RequiredArgsConstructor
@Component
public class NotificationMessageGenerator {

    private static final String EXPIRED_NOTIFICATION_TITLE = "유통기한 임박 알림 ⏰";
    private static final String EXHAUSTION_NOTIFICATION_TITLE = "재고 소진 알림 ⏰";

    public NotificationMessage generateForExpiredFoods(List<Food> foods, int days) {
        if (CollectionUtils.isEmpty(foods)) {
            throw new CoreException(ErrorType.EMPTY_NOTIFICATION_TARGET);
        }

        int count = foods.size();
        String foodName = foods.get(0).getName();
        String body = (count == 1)
            ? "'%s'의 소비기한이 %d일 남았습니다.".formatted(foodName, days)
            : "'%s' 외 %d개 품목의 소비기한이 %d일 남았습니다.".formatted(foodName, count - 1, days);

        return new NotificationMessage(EXPIRED_NOTIFICATION_TITLE, body);
    }

    public NotificationMessage generateForExhaustion(String foodName) {
        String body = StringUtils.hasText(foodName) ? foodName : "해당 품목의";
        return new NotificationMessage(EXHAUSTION_NOTIFICATION_TITLE, "%s 재고가 모두 소진되었습니다. 장바구니에 담으시겠어요?".formatted(body));
    }

}