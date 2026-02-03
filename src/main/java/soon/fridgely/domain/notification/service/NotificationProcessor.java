package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
    private final NotificationSettingFinder notificationSettingFinder;
    private final NotificationMessageGenerator messageGenerator;
    private final NotificationSender notificationSender;

    /**
     * 유통기한 임박 알림 처리
     */
    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void processExpiration(long memberId) {
        try {
            NotificationSetting setting = notificationSettingFinder.findNotificationSetting(memberId);
            if (!setting.isEnabled()) {
                return;
            }

            int daysBefore = setting.getAlertSchedule().getDaysBeforeExpiration();
            LocalDate targetDate = LocalDate.now().plusDays(daysBefore);

            List<Food> expiringFoods = foodFinder.findMyFoodsExpiringOnDate(memberId, targetDate);
            if (expiringFoods.isEmpty()) {
                return;
            }

            NotificationMessage message = messageGenerator.generateForExpiredFoods(expiringFoods, daysBefore);
            notificationSender.send(memberId, message.title(), message.body());
        } catch (Exception e) {
            log.error("[Notification] 유통기한 알림 처리 중 오류 발생. (MemberId={})", memberId, e);
        }
    }

    /**
     * 재고 소진 알림 처리
     * 매일 정해진 시간에 실행됨
     */
    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void processStockSummary(long memberId) {
        try {
            NotificationSetting setting = notificationSettingFinder.findNotificationSetting(memberId);
            if (!setting.isEnabled()) {
                return;
            }

            List<Food> outOfStockFoods = foodFinder.findAllOutOfStock(memberId);
            if (outOfStockFoods.isEmpty()) {
                return;
            }

            NotificationMessage message = messageGenerator.generateForOutOfStockSummary(outOfStockFoods);
            notificationSender.send(memberId, message.title(), message.body());
        } catch (Exception e) {
            log.error("[Notification] 재고 소진 알림 처리 중 오류 발생 (MemberId={})", memberId, e);
        }
    }

}