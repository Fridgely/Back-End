package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.notification.dto.command.NotificationMessage;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.logging.SlackMarkers;
import soon.fridgely.global.support.notification.NotificationSender;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationProcessor {

    private static final String EXPIRED_NOTIFICATION_TITLE = "유통기한 임박 알림 ⏰";
    private static final String EXHAUSTION_NOTIFICATION_TITLE = "재고 소진 알림 ⏰";

    private final FoodRepository foodRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationSender notificationSender;

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void processExpiration(long memberId) {
        processNotification(
            memberId,
            "유통기한 알림",
            setting -> {
                LocalDate targetDate = setting.getAlertSchedule().getExpirationTargetDate(LocalDate.now());
                return foodRepository.findMyFoodsExpiringBetween(
                    memberId,
                    TimeRangeUtils.startOfDay(targetDate),
                    TimeRangeUtils.endOfDay(targetDate)
                );
            },
            (setting, foods) -> generateForExpiredFoods(foods, setting.getAlertSchedule().getDaysBeforeExpiration())
        );
    }

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void processStockSummary(long memberId) {
        processNotification(
            memberId,
            "재고 소진 알림",
            setting -> foodRepository.findAllOutOfStock(memberId),
            (setting, foods) -> generateForOutOfStockSummary(foods)
        );
    }

    private void processNotification(
        long memberId,
        String errorLabel,
        Function<NotificationSetting, List<Food>> foodFetcher,
        BiFunction<NotificationSetting, List<Food>, NotificationMessage> messageGenerator
    ) {
        try {
            NotificationSetting setting = notificationSettingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
            if (!setting.isEnabled()) {
                return;
            }

            List<Food> foods = foodFetcher.apply(setting);
            if (foods.isEmpty()) {
                return;
            }

            NotificationMessage message = messageGenerator.apply(setting, foods);
            notificationSender.send(memberId, message.title(), message.body());
        } catch (CoreException e) {
            log.error("[Notification] {} 처리 중 오류 발생. (MemberId={})", errorLabel, memberId, e);
        } catch (Exception e) {
            log.error(SlackMarkers.SYSTEM, "[Notification] {} 처리 중 시스템 오류 발생. (MemberId={})", errorLabel, memberId, e);
        }
    }

    private NotificationMessage generateForExpiredFoods(List<Food> foods, int days) {
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

    private NotificationMessage generateForOutOfStockSummary(List<Food> foods) {
        if (CollectionUtils.isEmpty(foods)) {
            throw new CoreException(ErrorType.EMPTY_NOTIFICATION_TARGET);
        }

        String mainFoodName = foods.get(0).getName();
        int count = foods.size();
        String body = (count == 1)
            ? "%s 재고가 모두 소진되었습니다.".formatted(mainFoodName)
            : "%s 외 %d건의 재고가 없습니다.".formatted(mainFoodName, count - 1);

        return new NotificationMessage(EXHAUSTION_NOTIFICATION_TITLE, body);
    }

}
