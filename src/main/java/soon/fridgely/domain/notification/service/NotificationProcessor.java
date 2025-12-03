package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.service.FoodFinder;
import soon.fridgely.domain.notification.dto.command.NotificationMessage;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.global.support.notification.NotificationSender;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationProcessor {

    private final FoodFinder foodFinder;
    private final NotificationMessageGenerator messageGenerator;
    private final NotificationSender notificationSender;

    @Transactional(readOnly = true)
    public void process(NotificationSetting setting) {
        try {
            long memberId = setting.getMember().getId();
            int daysBefore = setting.getAlertSchedule().daysBeforeExpiration();
            LocalDate targetDate = LocalDate.now().plusDays(daysBefore);

            List<Food> expiringFoods = foodFinder.findMyFoodsExpiringOnDate(memberId, targetDate);
            if (expiringFoods.isEmpty()) {
                return;
            }

            NotificationMessage message = messageGenerator.generate(expiringFoods, daysBefore);
            notificationSender.send(memberId, message.title(), message.body());
        } catch (Exception e) {
            log.error("[Notification] 알림 처리 중 오류 발생. (MemberId={})", setting.getMember().getId(), e);
        }
    }

}